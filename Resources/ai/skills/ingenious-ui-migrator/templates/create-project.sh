#!/usr/bin/env bash
#
# create-project.sh — INGenious (v3.1.x) project scaffolder for macOS / Linux.
#
# Creates an independent YAML-based INGenious project (YAML Object Repository,
# YAML Reusable Components, YAML Test Cases, YAML Test Lab) from the bundled
# `project-template`, then generates a `.project` (schema 3.1.0) metadata file by
# scanning the project's TestPlan and ReusableComponents folders.
#
# Requires only a POSIX shell + coreutils (bash, find, grep, sed, awk) — no
# Node, no extra runtime. Compatible with the macOS default bash 3.2.
#
# Usage:
#   ./create-project.sh --name <ProjectName> [options]
#
# Options:
#   --name <name>            (required) Project name / folder name.
#   --projects-root <dir>    Output root for projects (default: "Projects").
#   --sync                   Only (re)generate .project from existing files.
#   --no-samples             Scaffold an empty skeleton (omit Sample* content).
#   --with-env-helpers       Add GetEnv reusable components (env/URL selection).
#   --with-db-helpers        Add DatabaseConnection reusable component.
#   --scenarios a,b,c        Extra (empty) scenarios to register in .project.
#   --tags @x,@y             Extra tags to register in .project.
#
# Examples:
#   ./create-project.sh --name LoginSuite
#   ./create-project.sh --name LoginSuite --no-samples --with-env-helpers
#   ./create-project.sh --name LoginSuite --sync

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_TEMPLATE="$SCRIPT_DIR/project-template"
OPTIONAL_HELPERS="$SCRIPT_DIR/optional-helpers"

NAME=""
PROJECTS_ROOT="Projects"
SYNC=0
SAMPLES=1
ENV_HELPERS=0
DB_HELPERS=0
SCENARIOS=()
TAGS=()

usage() { sed -n '3,30p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

die() { echo "Error: $*" >&2; exit 1; }

# --- argument parsing -------------------------------------------------------
while [ $# -gt 0 ]; do
  case "$1" in
    --name) NAME="$2"; shift 2;;
    --projects-root) PROJECTS_ROOT="$2"; shift 2;;
    --sync) SYNC=1; shift;;
    --no-samples) SAMPLES=0; shift;;
    --with-env-helpers) ENV_HELPERS=1; shift;;
    --with-db-helpers) DB_HELPERS=1; shift;;
    --scenarios) IFS=',' read -ra SCENARIOS <<< "$2"; shift 2;;
    --tags) IFS=',' read -ra TAGS <<< "$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) die "Unknown argument: $1 (use --help)";;
  esac
done

[ -n "$NAME" ] || die "Missing required --name <ProjectName>"

PROJECT_PATH="$PROJECTS_ROOT/$NAME"

# --- helpers ----------------------------------------------------------------

# JSON-escape a string (backslash and double-quote only; names are plain text).
json_escape() {
  local s="$1"
  s="${s//\\/\\\\}"
  s="${s//\"/\\\"}"
  printf '%s' "$s"
}

# Read a single top-level scalar header field (scenario/testCase/reusable/name),
# stripping surrounding quotes. Stops before the steps block.
yaml_field() { # file key
  awk -v key="$2" '
    $0 ~ "^steps:[[:space:]]*$" { exit }
    {
      pat = "^" key ":[[:space:]]*"
      if ($0 ~ pat) {
        sub(pat, "", $0)
        sub(/[[:space:]]+$/, "", $0)
        sub(/^"/, "", $0); sub(/"$/, "", $0)
        sub(/^'\''/, "", $0); sub(/'\''$/, "", $0)
        print $0
        exit
      }
    }
  ' "$1"
}

# Read the tags list (one tag per output line), stripping quotes.
yaml_tags() { # file
  awk '
    $0 ~ "^steps:[[:space:]]*$" { exit }
    intag == 1 {
      if ($0 ~ /^[[:space:]]+-[[:space:]]*/) {
        line = $0
        sub(/^[[:space:]]+-[[:space:]]*/, "", line)
        sub(/[[:space:]]+$/, "", line)
        sub(/^"/, "", line); sub(/"$/, "", line)
        sub(/^'\''/, "", line); sub(/'\''$/, "", line)
        print line
        next
      } else { intag = 0 }
    }
    $0 ~ "^tags:[[:space:]]*$" { intag = 1 }
  ' "$1"
}

# Append a value to a newline-delimited unique list variable (by name).
add_unique() { # listvar value
  local cur; eval "cur=\"\${$1}\""
  case "
$cur
" in
    *"
$2
"*) ;; # already present
    *) if [ -z "$cur" ]; then eval "$1=\"\$2\""; else eval "$1=\"\$cur
\$2\""; fi;;
  esac
}

# --- .project generation ----------------------------------------------------
SCN_LIST=""     # newline-delimited unique scenario names
TAG_LIST=""     # newline-delimited unique tag names
DATA_OBJS=""    # newline-delimited records: type \t scenario \t name \t csvTags

collect() { # dir type
  local dir="$1" type="$2" f scn name tags csv t
  [ -d "$dir" ] || return 0
  while IFS= read -r f; do
    [ -n "$f" ] || continue
    scn="$(yaml_field "$f" scenario)"
    name="$(yaml_field "$f" testCase)"
    [ -n "$name" ] || name="$(yaml_field "$f" reusable)"
    [ -n "$name" ] || name="$(yaml_field "$f" name)"
    [ -n "$name" ] || name="$(basename "$f" | sed -E 's/\.ya?ml$//')"
    [ -n "$scn" ] || scn="$(basename "$(dirname "$f")")"

    csv=""
    while IFS= read -r t; do
      [ -n "$t" ] || continue
      add_unique TAG_LIST "$t"
      if [ -z "$csv" ]; then csv="$t"; else csv="$csv,$t"; fi
    done <<EOF
$(yaml_tags "$f")
EOF

    add_unique SCN_LIST "$scn"
    DATA_OBJS="$DATA_OBJS$type	$scn	$name	$csv
"
  done <<EOF
$(find "$dir" -type f \( -name '*.yaml' -o -name '*.yml' \) | LC_ALL=C sort)
EOF
}

write_project() {
  SCN_LIST=""; TAG_LIST=""; DATA_OBJS=""
  collect "$PROJECT_PATH/TestPlan" testcase
  collect "$PROJECT_PATH/ReusableComponents" reusable

  local s t
  for s in "${SCENARIOS[@]+"${SCENARIOS[@]}"}"; do [ -n "$s" ] && add_unique SCN_LIST "$s"; done
  for t in "${TAGS[@]+"${TAGS[@]}"}"; do [ -n "$t" ] && add_unique TAG_LIST "$t"; done

  local file="$PROJECT_PATH/.project"
  local nameEsc; nameEsc="$(json_escape "$NAME")"

  {
    printf '{\n'
    printf '  "id": "%s",\n' "$nameEsc"
    printf '  "name": "%s",\n' "$nameEsc"
    printf '  "version": "3.1.0",\n'
    printf '  "attributes": [],\n'
    printf '  "tags": [],\n'
    printf '  "_meta": [\n'

    # base scenario attribute
    printf '    {\n'
    printf '      "type": "attribute",\n'
    printf '      "name": "scenario",\n'
    printf '      "desc": "High level classification of test requirement/cases grouped together",\n'
    printf '      "ref": "com.ing.datalib.model.Attribute",\n'
    printf '      "attributes": [],\n'
    printf '      "tags": []\n'
    printf '    }'

    # tags
    while IFS= read -r t; do
      [ -n "$t" ] || continue
      printf ',\n    {\n'
      printf '      "type": "tag",\n'
      printf '      "name": "%s",\n' "$(json_escape "$t")"
      if [ "$t" = "@smoke" ]; then
        printf '      "desc": "Non-exhaustive set of tests that aim at ensuring that the most important functions work",\n'
      fi
      printf '      "ref": "com.ing.datalib.model.Tag",\n'
      printf '      "attributes": [],\n'
      printf '      "tags": []\n'
      printf '    }'
    done <<EOF
$TAG_LIST
EOF

    # scenarios
    while IFS= read -r s; do
      [ -n "$s" ] || continue
      printf ',\n    {\n'
      printf '      "type": "scenario",\n'
      printf '      "name": "%s",\n' "$(json_escape "$s")"
      printf '      "ref": "com.ing.datalib.model.Attribute",\n'
      printf '      "attributes": [],\n'
      printf '      "tags": []\n'
      printf '    }'
    done <<EOF
$SCN_LIST
EOF

    printf '\n  ],\n'
    printf '  "data": ['

    local first=1 line type scn name csv tag
    while IFS='	' read -r type scn name csv; do
      [ -n "$type" ] || continue
      if [ $first -eq 1 ]; then first=0; printf '\n'; else printf ',\n'; fi
      printf '    {\n'
      printf '      "id": "%s#%s",\n' "$(json_escape "$scn")" "$(json_escape "$name")"
      printf '      "name": "%s",\n' "$(json_escape "$name")"
      if [ -n "$csv" ]; then
        printf '      "tags": [ '
        local tfirst=1
        local OLDIFS="$IFS"; IFS=','
        for tag in $csv; do
          if [ $tfirst -eq 1 ]; then tfirst=0; else printf ', '; fi
          printf '{ "value": "%s" }' "$(json_escape "$tag")"
        done
        IFS="$OLDIFS"
        printf ' ],\n'
      else
        printf '      "tags": [],\n'
      fi
      printf '      "attributes": [\n'
      printf '        { "name": "type", "value": "%s" },\n' "$type"
      printf '        { "name": "scenario", "value": "%s" }\n' "$(json_escape "$scn")"
      printf '      ]\n'
      printf '    }'
    done <<EOF
$DATA_OBJS
EOF

    if [ $first -eq 1 ]; then printf '\n  ]\n'; else printf '\n  ]\n'; fi
    printf '}\n'
  } > "$file"

  PROJECT_FILE="$file"
}

count_data() {
  [ -n "$DATA_OBJS" ] || { echo 0; return; }
  printf '%s' "$DATA_OBJS" | grep -c "$(printf '\t')"
}

# --- scaffolding ------------------------------------------------------------
copy_template() {
  [ -d "$PROJECT_TEMPLATE" ] || die "Missing template bundle: $PROJECT_TEMPLATE"
  mkdir -p "$PROJECT_PATH"
  cp -R "$PROJECT_TEMPLATE/." "$PROJECT_PATH/"
  # Drop placeholders and the template .project (regenerated by scan).
  find "$PROJECT_PATH" -name '.gitkeep' -type f -delete
  rm -f "$PROJECT_PATH/.project"
  # Substitute the project-name token in any copied text file.
  local f
  grep -rlI '{{PROJECT_NAME}}' "$PROJECT_PATH" 2>/dev/null | while IFS= read -r f; do
    sed -i.bak "s/{{PROJECT_NAME}}/$NAME/g" "$f" && rm -f "$f.bak"
  done
}

remove_samples() {
  rm -rf \
    "$PROJECT_PATH/ObjectRepository/Web/SamplePage.yaml" \
    "$PROJECT_PATH/ReusableComponents/Common/Launch.yaml" \
    "$PROJECT_PATH/ReusableComponents/SampleScenario" \
    "$PROJECT_PATH/TestPlan/SampleScenario" \
    "$PROJECT_PATH/TestLab/SampleRelease" \
    "$PROJECT_PATH/TestData/SampleData.csv"
  mkdir -p \
    "$PROJECT_PATH/ObjectRepository/Web" \
    "$PROJECT_PATH/ReusableComponents" \
    "$PROJECT_PATH/TestPlan" \
    "$PROJECT_PATH/TestLab"
}

# --- main -------------------------------------------------------------------
if [ "$SYNC" -eq 1 ]; then
  [ -d "$PROJECT_PATH" ] || die "Cannot --sync: project not found at $PROJECT_PATH"
  write_project
  echo "Synced metadata ($(count_data) entries): $PROJECT_FILE"
  exit 0
fi

if [ -d "$PROJECT_PATH" ] && [ -n "$(ls -A "$PROJECT_PATH" 2>/dev/null)" ]; then
  die "Refusing to scaffold over a non-empty folder: $PROJECT_PATH
Use --sync to regenerate .project, or choose a different --name."
fi

copy_template

if [ "$SAMPLES" -eq 0 ]; then
  remove_samples
fi

if [ "$ENV_HELPERS" -eq 1 ]; then
  mkdir -p "$PROJECT_PATH/ReusableComponents/GetEnv"
  cp -R "$OPTIONAL_HELPERS/ReusableComponents/GetEnv/." "$PROJECT_PATH/ReusableComponents/GetEnv/"
fi

if [ "$DB_HELPERS" -eq 1 ]; then
  mkdir -p "$PROJECT_PATH/ReusableComponents/DatabaseConnection"
  cp -R "$OPTIONAL_HELPERS/ReusableComponents/DatabaseConnection/." "$PROJECT_PATH/ReusableComponents/DatabaseConnection/"
fi

for s in "${SCENARIOS[@]+"${SCENARIOS[@]}"}"; do
  [ -n "$s" ] || continue
  mkdir -p "$PROJECT_PATH/TestPlan/$s" "$PROJECT_PATH/ReusableComponents/$s"
done

write_project

echo "Created INGenious project: $PROJECT_PATH"
echo "  scenarios : $(printf '%s' "$SCN_LIST" | paste -sd ',' - 2>/dev/null)"
echo "  entries   : $(count_data)"
echo "  .project  : $PROJECT_FILE"
echo "Next: add YAML pages to ObjectRepository/Web, reusables to ReusableComponents/<Scenario>, test cases to TestPlan/<Scenario>, then re-run with --sync to refresh .project."
