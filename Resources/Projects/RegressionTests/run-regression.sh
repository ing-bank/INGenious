#!/usr/bin/env bash
# Test Data scope reference regression - runs the TestDataScope scenario headless and
# validates the two artefacts (rendered template + resolved DB-connection config).
# Run from a built Dist/release.
#
#   Projects/RegressionTests/run-regression.sh            # cwd = Dist/release
#   JAVA=/path/to/jdk-17/bin/java Projects/RegressionTests/run-regression.sh
set -u

DIST="${DIST:-$(cd "$(dirname "$0")/../.." && pwd)}"   # .../Dist/release
JAVA="${JAVA:-java}"
# classpath separator: ';' on Windows (incl. Git Bash), ':' elsewhere
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';';; *) SEP=':';; esac
CP="lib/*${SEP}lib/clib/*"
[ -d "$DIST/lib" ] || { echo "ERROR: '$DIST/lib' not found - point DIST at a built Dist/release." >&2; exit 2; }
cd "$DIST" || exit 2

run() {  # run <testcase>  -> echoes engine output
  "$JAVA" -Xms64m -Xmx768m -Dfile.encoding=UTF-8 -cp "$CP" \
      com.ing.engine.core.Control run "RegressionTests/TestDataScope/$1" 2>&1
}

RENDERED="$DIST/regtest-rendered.txt"
rm -f "$RENDERED"
fail=0

# --- self-asserting test cases -------------------------------------------------
for tc in AllReferenceForms WriteBack RenderTemplate SyntheticDataWriteBack PreviousTestCaseWriteBack; do
  echo "=========================================================================="
  echo "  $tc"
  echo "=========================================================================="
  out="$(run "$tc")"
  echo "$out" | grep -E "Passed Steps|Failed Steps|Time Taken" | head -2
  if echo "$out" | grep -qE "Status: PASS"; then echo "  -> $tc PASS"; else echo "  -> $tc FAIL"; fail=1; fi
done

# --- rendered template (FileOperations -> resolveEmbeddedTokens) --------------
echo "=========================================================================="
echo "  rendered-template check ($RENDERED)"
echo "=========================================================================="
expected='line1 PROJVAL | line2 PROJVAL | line3 SHRVAL'
if [ -f "$RENDERED" ] && [ "$(head -1 "$RENDERED")" = "$expected" ]; then
  cat "$RENDERED"; echo "  -> rendered-template PASS"
else
  echo "  -> rendered-template FAIL"
  echo "     expected: $expected"
  echo "     actual  : $( [ -f "$RENDERED" ] && head -1 "$RENDERED" || echo '<file not produced>')"
  fail=1
fi

# --- DB connection config (Settings -> resolveAllVariables) ------------------
# One property MIXES literal + %var% + {[Project]} + {[Shared]}. Step FAILS by design
# (no JDBC driver); we key off the fully-composed marker in the error message.
echo "=========================================================================="
echo "  db-connection-config check (literal + %var% + {[Project]} + {[Shared]} in one value)"
echo "=========================================================================="
out="$(run DbConnectionConfig)"
msg="$(echo "$out" | grep -E "Error connecting Database:" | head -1)"
echo "  $msg"
if echo "$msg" | grep -q "No suitable driver found for jdbc:regmark:proj-ok-LIVE-SHRVAL" \
   && ! echo "$msg" | grep -qE "\{\[(Project|Shared)\]|%dbEnv%"; then
  echo "  -> db-connection-config PASS  (mixed literal + %var% + [Project] + [Shared] resolved)"
else
  echo "  -> db-connection-config FAIL  (config value not fully resolved)"
  fail=1
fi

echo "=========================================================================="
[ "$fail" -eq 0 ] && echo "  REGRESSION: PASS" || echo "  REGRESSION: FAIL"
exit "$fail"
