#!/usr/bin/env python3
"""
categorize_changes.py

Purpose: Categorize customizations by analyzing change patterns
Usage: python3 categorize_changes.py <diff_file>

Analyzes diff content and assigns categories based on pattern matching:
- Feature Enhancement
- Bug Fix
- Configuration
- Integration
- Performance
- UI/Reporting
- Framework Core
"""

import sys
import re
from typing import Dict, List, Tuple


# Category patterns for matching
CATEGORY_PATTERNS = {
    "Feature Enhancement": [
        r"new class", r"new method", r"implements", r"extends",
        r"Added functionality", r"Enhanced", r"@Action", r"@Command"
    ],
    "Bug Fix": [
        r"fix", r"null check", r"exception", r"try-catch", r"validate",
        r"NullPointerException", r"fixed", r"bug", r"issue"
    ],
    "Configuration": [
        r"pom\.xml", r"dependency", r"plugin", r"<version>", r"\.properties",
        r"configuration", r"settings"
    ],
    "Integration": [
        r"API", r"REST", r"external", r"third-party", r"integration",
        r"client", r"service"
    ],
    "Performance": [
        r"optimize", r"cache", r"performance", r"faster", r"efficient",
        r"memory", r"speed"
    ],
    "UI/Reporting": [
        r"report", r"template", r"HTML", r"dashboard", r"display", r"UI",
        r"render", r"view"
    ],
    "Framework Core": [
        r"engine", r"core", r"framework", r"architecture", r"Plugin",
        r"driver", r"executor"
    ]
}


def categorize_change(content: str) -> Tuple[str, int]:
    """
    Categorize a change based on content analysis.
    
    Returns: (category_name, confidence_score)
    """
    scores = {category: 0 for category in CATEGORY_PATTERNS}
    
    # Convert content to lowercase for case-insensitive matching
    content_lower = content.lower()
    
    for category, patterns in CATEGORY_PATTERNS.items():
        for pattern in patterns:
            matches = len(re.findall(pattern, content_lower, re.IGNORECASE))
            scores[category] += matches
    
    # Find category with highest score
    if max(scores.values()) == 0:
        return "Uncategorized", 0
    
    best_category = max(scores.items(), key=lambda x: x[1])
    return best_category


def analyze_diff_file(diff_path: str) -> Dict[str, List[str]]:
    """
    Analyze a diff file and categorize all changes.
    
    Returns: Dictionary mapping categories to list of file changes
    """
    try:
        with open(diff_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except FileNotFoundError:
        print(f"Error: File not found: {diff_path}")
        sys.exit(1)
    
    categorized = {cat: [] for cat in CATEGORY_PATTERNS}
    categorized["Uncategorized"] = []
    
    # Split by diff sections
    diff_sections = re.split(r'^diff ', content, flags=re.MULTILINE)
    
    for section in diff_sections[1:]:  # Skip first empty section
        # Extract filename
        file_match = re.search(r'^---\s+(.+)$', section, re.MULTILINE)
        if not file_match:
            continue
        
        filename = file_match.group(1)
        
        # Categorize this section
        category, score = categorize_change(section)
        categorized[category].append({
            'file': filename,
            'score': score
        })
    
    return categorized


def print_summary(categorized: Dict[str, List[str]]):
    """Print categorization summary."""
    print("\n=== Customization Categorization Summary ===\n")
    
    total_changes = sum(len(files) for files in categorized.values())
    print(f"Total changes analyzed: {total_changes}\n")
    
    for category, files in sorted(categorized.items()):
        if files:
            print(f"{category}: {len(files)} changes")
            for item in files[:5]:  # Show first 5
                print(f"  - {item['file']} (score: {item['score']})")
            if len(files) > 5:
                print(f"  ... and {len(files) - 5} more")
            print()


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 categorize_changes.py <diff_file>")
        sys.exit(1)
    
    diff_file = sys.argv[1]
    categorized = analyze_diff_file(diff_file)
    print_summary(categorized)
