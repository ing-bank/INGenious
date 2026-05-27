#!/usr/bin/env python3
"""
generate_plugin_spec.py

Purpose: Generate plugin specifications from detected customizations
Usage: python3 generate_plugin_spec.py <customization_data.json>

Analyzes customization data and generates structured plugin specifications
that can be used with the ingenious-plugin-creation skill.
"""

import sys
import json
import re
from typing import Dict, List, Any


# Plugin type detection patterns
PLUGIN_TYPE_PATTERNS = {
    "browser": [
        r"com\.microsoft\.playwright",
        r"playwright",
        r"browser",
        r"page",
        r"locator"
    ],
    "mobile": [
        r"io\.appium",
        r"appium",
        r"mobile",
        r"android",
        r"ios"
    ],
    "webservice": [
        r"java\.net\.http",
        r"HttpClient",
        r"REST",
        r"API",
        r"request",
        r"response"
    ],
    "database": [
        r"java\.sql",
        r"jdbc",
        r"database",
        r"connection",
        r"query"
    ]
}

# API contracts mapping
API_CONTRACTS = {
    "browser": "BrowserPluginApi",
    "mobile": "MobilePluginApi",
    "webservice": "WebservicePluginApi",
    "database": "DatabasePluginApi",
    "general": "CommandPluginApi"
}


def detect_plugin_type(imports: List[str], code: str) -> str:
    """Detect plugin type based on imports and code content."""
    scores = {ptype: 0 for ptype in PLUGIN_TYPE_PATTERNS}
    
    all_text = " ".join(imports) + " " + code
    
    for ptype, patterns in PLUGIN_TYPE_PATTERNS.items():
        for pattern in patterns:
            matches = len(re.findall(pattern, all_text, re.IGNORECASE))
            scores[ptype] += matches
    
    # Return type with highest score, or 'general' if no match
    if max(scores.values()) == 0:
        return "general"
    
    return max(scores.items(), key=lambda x: x[1])[0]


def infer_plugin_name(customization_data: Dict) -> str:
    """Infer a plugin name from customization data."""
    # Use first file path or default name
    if 'files' in customization_data and customization_data['files']:
        first_file = customization_data['files'][0]
        # Extract meaningful name from file path
        name_match = re.search(r'/(\w+)\.java$', first_file)
        if name_match:
            return name_match.group(1).lower() + "-plugin"
    
    return "custom-actions"


def extract_actions(methods: List[Dict]) -> List[Dict]:
    """Extract action specifications from method data."""
    actions = []
    
    for method in methods:
        action = {
            "method_name": method.get('name', 'unknownAction'),
            "description": method.get('javadoc', f"Custom action: {method.get('name')}"),
            "object_type": infer_object_type(method),
            "input_required": has_data_parameter(method),
            "condition_optional": has_condition_parameter(method),
            "source_file": method.get('file_path', ''),
            "source_lines": f"{method.get('start_line', 0)}-{method.get('end_line', 0)}"
        }
        actions.append(action)
    
    return actions


def infer_object_type(method: Dict) -> str:
    """Infer object type from method signature."""
    signature = method.get('signature', '')
    
    if 'Page' in signature or 'Locator' in signature:
        return 'PLAYWRIGHT'
    elif 'Driver' in signature or 'WebElement' in signature:
        return 'APPIUM'
    elif 'HttpClient' in signature or 'Request' in signature:
        return 'HTTP'
    elif 'Connection' in signature or 'Statement' in signature:
        return 'JDBC'
    
    return 'GENERIC'


def has_data_parameter(method: Dict) -> bool:
    """Check if method has a Data parameter."""
    params = method.get('parameters', [])
    return any('Data' in p for p in params)


def has_condition_parameter(method: Dict) -> bool:
    """Check if method has a Condition parameter."""
    params = method.get('parameters', [])
    return any('Condition' in p for p in params)


def detect_dependencies(imports: List[str]) -> List[Dict]:
    """Detect external dependencies from imports."""
    dependencies = []
    
    # Common dependency patterns
    dependency_map = {
        'org.apache.poi': {'groupId': 'org.apache.poi', 'artifactId': 'poi', 'version': '5.2.3'},
        'com.google.gson': {'groupId': 'com.google.code.gson', 'artifactId': 'gson', 'version': '2.10.1'},
        'org.json': {'groupId': 'org.json', 'artifactId': 'json', 'version': '20231013'},
    }
    
    for imp in imports:
        for key, dep in dependency_map.items():
            if key in imp:
                if dep not in dependencies:
                    dependencies.append(dep)
    
    return dependencies


def generate_plugin_spec(customization_data: Dict) -> Dict:
    """Generate complete plugin specification from customization data."""
    
    imports = customization_data.get('imports', [])
    code = customization_data.get('code', '')
    methods = customization_data.get('new_methods', [])
    
    plugin_type = detect_plugin_type(imports, code)
    plugin_name = infer_plugin_name(customization_data)
    
    spec = {
        "plugin_name": plugin_name,
        "plugin_type": plugin_type,
        "api_contract": API_CONTRACTS.get(plugin_type, "CommandPluginApi"),
        "actions": extract_actions(methods),
        "dependencies": detect_dependencies(imports),
        "source_files": customization_data.get('files', [])
    }
    
    return spec


def main():
    if len(sys.argv) < 2:
        print("Usage: python3 generate_plugin_spec.py <customization_data.json>")
        sys.exit(1)
    
    input_file = sys.argv[1]
    
    try:
        with open(input_file, 'r', encoding='utf-8') as f:
            customization_data = json.load(f)
    except FileNotFoundError:
        print(f"Error: File not found: {input_file}")
        sys.exit(1)
    except json.JSONDecodeError:
        print(f"Error: Invalid JSON in {input_file}")
        sys.exit(1)
    
    spec = generate_plugin_spec(customization_data)
    
    # Output specification
    output_file = "plugin_specification.json"
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(spec, f, indent=2)
    
    print(f"✓ Plugin specification generated: {output_file}")
    print(f"\nPlugin Name: {spec['plugin_name']}")
    print(f"Plugin Type: {spec['plugin_type']}")
    print(f"API Contract: {spec['api_contract']}")
    print(f"Actions: {len(spec['actions'])}")
    print(f"Dependencies: {len(spec['dependencies'])}")


if __name__ == "__main__":
    main()
