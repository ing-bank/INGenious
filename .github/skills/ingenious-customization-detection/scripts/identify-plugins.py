#!/usr/bin/env python3
"""
Identify plugin candidates from customizations
Usage: ./identify-plugins.py <diff-dir> <user-path> <output-file>
"""

import sys
import os
import re
import json
from pathlib import Path

def detect_plugin_type(imports, file_path):
    """Detect plugin type based on imports and file location"""
    
    imports_text = ' '.join(imports).lower()
    path_lower = file_path.lower()
    
    if 'playwright' in imports_text or 'browser' in path_lower:
        return 'browser', 'BrowserPluginApi'
    elif 'appium' in imports_text or 'mobile' in path_lower:
        return 'mobile', 'MobilePluginApi'
    elif 'httplient' in imports_text or 'webclient' in imports_text:
        return 'webservice', 'WebservicePluginApi'
    elif 'java.sql' in imports_text or 'jdbc' in imports_text:
        return 'database', 'DatabasePluginApi'
    else:
        return 'general', 'CommandPluginApi'

def extract_methods(diff_content):
    """Extract method signatures from diff content"""
    
    methods = []
    lines = diff_content.split('\n')
    
    for i, line in enumerate(lines):
        if line.startswith('+') and 'public' in line and '(' in line:
            # Potential method
            method_match = re.search(r'public\s+(void|boolean|String)\s+(\w+)\s*\(([^)]*)\)', line)
            if method_match:
                return_type = method_match.group(1)
                method_name = method_match.group(2)
                params = method_match.group(3)
                
                # Skip getters, setters, standard methods
                if method_name.startswith(('get', 'set', 'is', 'equals', 'hashCode', 'toString')):
                    continue
                
                methods.append({
                    'name': method_name,
                    'return_type': return_type,
                    'params': params,
                    'line_number': i + 1
                })
    
    return methods

def is_new_file(diff_content):
    """Check if this is a completely new file"""
    return diff_content.startswith('--- /dev/null') or 'new file mode' in diff_content

def extract_imports(diff_content):
    """Extract import statements from diff"""
    
    imports = []
    for line in diff_content.split('\n'):
        if line.startswith('+import '):
            imports.append(line[1:].strip())
    return imports

def identify_plugins(diff_dir, user_path, output_file):
    """Identify plugin candidates from diff files"""
    
    plugins = []
    
    # Process diff files
    for diff_file in Path(diff_dir).glob('diff_*.patch'):
        module_name = diff_file.stem.replace('diff_', '')
        
        with open(diff_file, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        
        # Split by file
        file_diffs = re.split(r'^diff ', content, flags=re.MULTILINE)
        
        for file_diff in file_diffs:
            if not file_diff.strip():
                continue
            
            # Extract file path
            file_match = re.search(r'b/(.+?)(?:\n|$)', file_diff)
            if not file_match:
                continue
            
            file_path = file_match.group(1)
            
            # Skip if not a new file or significant addition
            if not is_new_file(file_diff):
                # Check if enough new lines
                new_lines = sum(1 for line in file_diff.split('\n') if line.startswith('+'))
                if new_lines < 20:  # Skip minor changes
                    continue
            
            # Extract imports and methods
            imports = extract_imports(file_diff)
            methods = extract_methods(file_diff)
            
            if not methods:
                continue
            
            # Detect plugin type
            plugin_type, api_contract = detect_plugin_type(imports, file_path)
            
            # Create plugin specification
            plugin_name = f"custom-{plugin_type}-actions"
            
            # Check if plugin already exists
            existing_plugin = next((p for p in plugins if p['name'] == plugin_name), None)
            
            if existing_plugin:
                # Add actions to existing plugin
                for method in methods:
                    existing_plugin['actions'].append({
                        'method_name': method['name'],
                        'description': f"Custom action: {method['name']}",
                        'return_type': method['return_type'],
                        'source_file': file_path,
                        'source_module': module_name
                    })
            else:
                # Create new plugin
                plugins.append({
                    'name': plugin_name,
                    'type': plugin_type,
                    'api_contract': api_contract,
                    'priority': 'high' if len(methods) > 3 else 'medium',
                    'complexity': 'complex' if len(methods) > 5 else 'moderate' if len(methods) > 2 else 'simple',
                    'actions': [{
                        'method_name': method['name'],
                        'description': f"Custom action: {method['name']}",
                        'return_type': method['return_type'],
                        'source_file': file_path,
                        'source_module': module_name
                    } for method in methods],
                    'dependencies': list(set(imports))
                })
    
    # Generate output
    if plugins:
        report = []
        report.append("# Plugin Extraction Recommendations\n")
        report.append(f"Based on customization analysis, **{len(plugins)} plugin(s)** can be extracted:\n")
        
        for i, plugin in enumerate(plugins, 1):
            priority_icon = "✅" if plugin['priority'] == 'high' else "⚠️"
            report.append(f"## Plugin {i}: {plugin['name'].replace('-', ' ').title()}\n")
            report.append(f"{priority_icon} **{plugin['priority'].title()} Priority** | "
                        f"**Type:** {plugin['type'].title()} | "
                        f"**Complexity:** {plugin['complexity'].title()}\n")
            report.append(f"**API Contract:** `{plugin['api_contract']}`\n")
            report.append(f"**Actions Identified:** {len(plugin['actions'])}\n")
            
            for action in plugin['actions'][:5]:  # Show first 5
                report.append(f"- `{action['method_name']}()` — {action['description']}")
                report.append(f"  - Source: `{action['source_file']}`")
            
            if len(plugin['actions']) > 5:
                report.append(f"\n*...and {len(plugin['actions']) - 5} more actions*\n")
            
            report.append("")
        
        report.append("## Plugin Specifications (JSON)\n")
        report.append("```json")
        report.append(json.dumps({'plugins': plugins}, indent=2))
        report.append("```")
        
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write('\n'.join(report))
        
        # Also save JSON
        json_file = output_file.replace('.md', '.json')
        with open(json_file, 'w', encoding='utf-8') as f:
            json.dump({'plugins': plugins}, f, indent=2)
        
        print(f"✓ Plugin analysis complete:")
        print(f"  - {len(plugins)} plugin(s) identified")
        print(f"  - Report: {output_file}")
        print(f"  - JSON spec: {json_file}")
    else:
        print("No significant plugin candidates found")
        # Create empty report
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("# Plugin Extraction Recommendations\n\n")
            f.write("No significant plugin candidates identified from the customizations.\n")

def main():
    if len(sys.argv) != 4:
        print("Usage: ./identify-plugins.py <diff-dir> <user-path> <output-file>")
        print("Example: ./identify-plugins.py ./diffs /path/to/user plugins.md")
        sys.exit(1)
    
    diff_dir = sys.argv[1]
    user_path = sys.argv[2]
    output_file = sys.argv[3]
    
    identify_plugins(diff_dir, user_path, output_file)

if __name__ == '__main__':
    main()
