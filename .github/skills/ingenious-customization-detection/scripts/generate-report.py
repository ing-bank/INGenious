#!/usr/bin/env python3
"""
Generate customization report from diff files
Usage: ./generate-report.py <diff-dir> <user-path> <version> <copy-type> <output-file>
"""

import sys
import os
import re
from pathlib import Path
from datetime import datetime

def categorize_change(file_path, diff_content):
    """Categorize a change based on file path and diff content"""
    
    categories = {
        "Feature Enhancement": [
            "new class", "new method", "implements", "extends",
            "Added functionality", "Enhanced", "public class", "public void"
        ],
        "Bug Fix": [
            "fix", "null check", "exception", "try-catch", "validate",
            "NullPointerException", "fixed", "catch (", "if (.*== null)"
        ],
        "Configuration": [
            "pom.xml", "dependency", "plugin", "version", "properties",
            "<dependency>", "<plugin>", ".properties"
        ],
        "Integration": [
            "API", "REST", "external", "third-party", "integration",
            "import", "HttpClient", "WebClient"
        ],
        "Performance": [
            "optimize", "cache", "performance", "faster", "efficient",
            "parallel", "async"
        ],
        "UI/Reporting": [
            "report", "template", "HTML", "dashboard", "display", "UI",
            ".html", ".css", "Report"
        ],
        "Framework Core": [
            "engine", "core", "framework", "architecture", "Plugin",
            "Engine/", "Datalib/"
        ]
    }
    
    # Check file path and content for patterns
    combined_text = f"{file_path} {diff_content}".lower()
    
    for category, patterns in categories.items():
        for pattern in patterns:
            if re.search(pattern.lower(), combined_text):
                return category
    
    return "Other"

def assess_impact(file_path, lines_added, lines_removed):
    """Assess impact level of a change"""
    
    # High impact indicators
    high_impact_paths = ["Engine/src/main/java/com/ing/engine/core",
                         "Engine/src/main/java/com/ing/engine/execution",
                         "Datalib/src", "API", "Plugin"]
    
    for path in high_impact_paths:
        if path.lower() in file_path.lower():
            return "High"
    
    # Medium impact: significant code changes
    if lines_added + lines_removed > 100:
        return "Medium"
    
    # Low impact
    return "Low"

def parse_diff_file(diff_file):
    """Parse a diff file and extract changes"""
    
    if not os.path.exists(diff_file):
        return []
    
    changes = []
    current_file = None
    current_diff = []
    
    with open(diff_file, 'r', encoding='utf-8', errors='ignore') as f:
        for line in f:
            if line.startswith('diff '):
                # Save previous file's diff
                if current_file:
                    changes.append({
                        'file': current_file,
                        'diff': '\n'.join(current_diff),
                        'lines_added': sum(1 for l in current_diff if l.startswith('+')),
                        'lines_removed': sum(1 for l in current_diff if l.startswith('-'))
                    })
                
                # Start new file
                current_file = line.split()[-1] if len(line.split()) > 1 else 'unknown'
                current_diff = []
            else:
                current_diff.append(line.rstrip())
        
        # Save last file
        if current_file:
            changes.append({
                'file': current_file,
                'diff': '\n'.join(current_diff),
                'lines_added': sum(1 for l in current_diff if l.startswith('+')),
                'lines_removed': sum(1 for l in current_diff if l.startswith('-'))
            })
    
    return changes

def generate_report(diff_dir, user_path, version, copy_type, output_file):
    """Generate the customization report"""
    
    report = []
    report.append("# INGenious Customization Report\n")
    report.append(f"**Analysis Date:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
    report.append("")
    report.append("## Summary\n")
    report.append(f"- **Analyzed Installation:** `{user_path}`")
    report.append(f"- **Version Detected:** {version}")
    report.append(f"- **Copy Type:** {copy_type}")
    report.append("")
    
    # Process all diff files
    all_changes = []
    modules_analyzed = []
    
    for diff_file in Path(diff_dir).glob('diff_*.patch'):
        module_name = diff_file.stem.replace('diff_', '')
        modules_analyzed.append(module_name)
        
        changes = parse_diff_file(str(diff_file))
        for change in changes:
            change['module'] = module_name
            change['category'] = categorize_change(change['file'], change['diff'])
            change['impact'] = assess_impact(change['file'], 
                                            change['lines_added'], 
                                            change['lines_removed'])
            all_changes.append(change)
    
    # Statistics
    total_files = len(all_changes)
    total_added = sum(c['lines_added'] for c in all_changes)
    total_removed = sum(c['lines_removed'] for c in all_changes)
    
    report.append("## Overview Statistics\n")
    report.append(f"- **Modules Analyzed:** {len(modules_analyzed)}")
    report.append(f"- **Modules:** {', '.join(modules_analyzed)}")
    report.append(f"- **Files Modified:** {total_files}")
    report.append(f"- **Total Lines Added:** +{total_added}")
    report.append(f"- **Total Lines Removed:** -{total_removed}")
    report.append("")
    
    # Group by category
    categories = {}
    for change in all_changes:
        cat = change['category']
        if cat not in categories:
            categories[cat] = []
        categories[cat].append(change)
    
    report.append("## Customizations by Category\n")
    
    for category, changes in sorted(categories.items()):
        report.append(f"### {category}")
        report.append(f"**Changes:** {len(changes)} files\n")
        
        for change in changes[:5]:  # Show first 5 per category
            report.append(f"#### {change['file']}")
            report.append(f"- **Module:** {change['module']}")
            report.append(f"- **Impact:** {change['impact']}")
            report.append(f"- **Changes:** +{change['lines_added']} / -{change['lines_removed']} lines")
            report.append("")
        
        if len(changes) > 5:
            report.append(f"*...and {len(changes) - 5} more files*\n")
        report.append("")
    
    report.append("## Risk Assessment\n")
    report.append("| Change Type | Risk Level | Count |")
    report.append("|-------------|-----------|-------|")
    
    high_impact = [c for c in all_changes if c['impact'] == 'High']
    medium_impact = [c for c in all_changes if c['impact'] == 'Medium']
    low_impact = [c for c in all_changes if c['impact'] == 'Low']
    
    if high_impact:
        report.append(f"| Core Framework Changes | **High** | {len(high_impact)} files |")
    if medium_impact:
        report.append(f"| Significant Modifications | Medium | {len(medium_impact)} files |")
    if low_impact:
        report.append(f"| Minor Changes | Low | {len(low_impact)} files |")
    
    report.append("")
    report.append("## Detailed Diff Files\n")
    report.append(f"Full diff files available at: `{diff_dir}/`\n")
    
    # Write report
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write('\n'.join(report))
    
    print(f"✓ Report generated: {output_file}")
    print(f"  - {total_files} files analyzed")
    print(f"  - {len(categories)} categories")
    print(f"  - {total_added} lines added, {total_removed} lines removed")

def main():
    if len(sys.argv) != 6:
        print("Usage: ./generate-report.py <diff-dir> <user-path> <version> <copy-type> <output-file>")
        print("Example: ./generate-report.py ./diffs /path/to/user 2.3 BUILD_COPY report.md")
        sys.exit(1)
    
    diff_dir = sys.argv[1]
    user_path = sys.argv[2]
    version = sys.argv[3]
    copy_type = sys.argv[4]
    output_file = sys.argv[5]
    
    generate_report(diff_dir, user_path, version, copy_type, output_file)

if __name__ == '__main__':
    main()
