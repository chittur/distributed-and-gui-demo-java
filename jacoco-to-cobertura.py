#!/usr/bin/env python3
"""
Convert JaCoCo XML coverage report to Cobertura XML format for better Azure DevOps visualization.
"""

import xml.etree.ElementTree as ET
import sys
import os
from pathlib import Path

def convert_jacoco_to_cobertura(jacoco_file, cobertura_file, source_dirs):
    """Convert JaCoCo XML to Cobertura XML format."""
    
    # Parse JaCoCo XML
    try:
        jacoco_tree = ET.parse(jacoco_file)
        jacoco_root = jacoco_tree.getroot()
    except ET.ParseError as e:
        print(f"Error parsing JaCoCo file: {e}")
        return False
    except FileNotFoundError:
        print(f"JaCoCo file not found: {jacoco_file}")
        return False

    # Create Cobertura XML structure
    cobertura_root = ET.Element("coverage")
    cobertura_root.set("line-rate", "0.0")
    cobertura_root.set("branch-rate", "0.0")
    cobertura_root.set("lines-covered", "0")
    cobertura_root.set("lines-valid", "0")
    cobertura_root.set("branches-covered", "0")
    cobertura_root.set("branches-valid", "0")
    cobertura_root.set("complexity", "0.0")
    cobertura_root.set("version", "1.0")
    cobertura_root.set("timestamp", "0")

    # Add sources
    sources = ET.SubElement(cobertura_root, "sources")
    for source_dir in source_dirs:
        source = ET.SubElement(sources, "source")
        source.text = source_dir

    # Add packages
    packages = ET.SubElement(cobertura_root, "packages")
    
    total_lines = 0
    covered_lines = 0
    total_branches = 0
    covered_branches = 0

    # Process JaCoCo packages
    for jacoco_package in jacoco_root.findall(".//package"):
        package_name = jacoco_package.get("name", "").replace("/", ".")
        
        # Create Cobertura package
        cobertura_package = ET.SubElement(packages, "package")
        cobertura_package.set("name", package_name)
        cobertura_package.set("line-rate", "0.0")
        cobertura_package.set("branch-rate", "0.0")
        cobertura_package.set("complexity", "0.0")
        
        # Add classes
        classes = ET.SubElement(cobertura_package, "classes")
        
        package_lines = 0
        package_covered_lines = 0
        package_branches = 0
        package_covered_branches = 0
        
        # Process classes in package
        for jacoco_class in jacoco_package.findall(".//class"):
            class_name = jacoco_class.get("name", "").split("/")[-1]
            
            # Create Cobertura class
            cobertura_class = ET.SubElement(classes, "class")
            cobertura_class.set("name", class_name)
            cobertura_class.set("filename", f"{package_name.replace('.', '/')}/{class_name}.java")
            cobertura_class.set("line-rate", "0.0")
            cobertura_class.set("branch-rate", "0.0")
            cobertura_class.set("complexity", "0.0")
            
            # Add methods
            methods = ET.SubElement(cobertura_class, "methods")
            
            # Add lines
            lines = ET.SubElement(cobertura_class, "lines")
            
            class_lines = 0
            class_covered_lines = 0
            class_branches = 0
            class_covered_branches = 0
            
            # Process line coverage
            for counter in jacoco_class.findall(".//counter[@type='LINE']"):
                missed = int(counter.get("missed", "0"))
                covered = int(counter.get("covered", "0"))
                
                class_lines += missed + covered
                class_covered_lines += covered
                
                # Create lines for visualization
                for i in range(1, missed + covered + 1):
                    line = ET.SubElement(lines, "line")
                    line.set("number", str(i))
                    line.set("hits", "1" if i <= covered else "0")
                    line.set("branch", "false")
            
            # Process branch coverage
            for counter in jacoco_class.findall(".//counter[@type='BRANCH']"):
                missed = int(counter.get("missed", "0"))
                covered = int(counter.get("covered", "0"))
                
                class_branches += missed + covered
                class_covered_branches += covered
            
            # Update class rates
            if class_lines > 0:
                class_line_rate = class_covered_lines / class_lines
                cobertura_class.set("line-rate", f"{class_line_rate:.4f}")
            
            if class_branches > 0:
                class_branch_rate = class_covered_branches / class_branches
                cobertura_class.set("branch-rate", f"{class_branch_rate:.4f}")
            
            package_lines += class_lines
            package_covered_lines += class_covered_lines
            package_branches += class_branches
            package_covered_branches += class_covered_branches
        
        # Update package rates
        if package_lines > 0:
            package_line_rate = package_covered_lines / package_lines
            cobertura_package.set("line-rate", f"{package_line_rate:.4f}")
        
        if package_branches > 0:
            package_branch_rate = package_covered_branches / package_branches
            cobertura_package.set("branch-rate", f"{package_branch_rate:.4f}")
        
        total_lines += package_lines
        covered_lines += package_covered_lines
        total_branches += package_branches
        covered_branches += package_covered_branches

    # Update overall rates
    if total_lines > 0:
        overall_line_rate = covered_lines / total_lines
        cobertura_root.set("line-rate", f"{overall_line_rate:.4f}")
        cobertura_root.set("lines-covered", str(covered_lines))
        cobertura_root.set("lines-valid", str(total_lines))
    
    if total_branches > 0:
        overall_branch_rate = covered_branches / total_branches
        cobertura_root.set("branch-rate", f"{overall_branch_rate:.4f}")
        cobertura_root.set("branches-covered", str(covered_branches))
        cobertura_root.set("branches-valid", str(total_branches))

    # Write Cobertura XML
    try:
        # Create directory if it doesn't exist
        os.makedirs(os.path.dirname(cobertura_file), exist_ok=True)
        
        # Pretty print XML
        ET.indent(cobertura_root, space="  ")
        cobertura_tree = ET.ElementTree(cobertura_root)
        
        with open(cobertura_file, 'wb') as f:
            f.write(b'<?xml version="1.0" encoding="UTF-8"?>\n')
            f.write(b'<!DOCTYPE coverage SYSTEM "http://cobertura.sourceforge.net/xml/coverage-04.dtd">\n')
            cobertura_tree.write(f, encoding='utf-8', xml_declaration=False)
        
        print(f"Successfully converted {jacoco_file} to {cobertura_file}")
        print(f"Coverage: {covered_lines}/{total_lines} lines ({overall_line_rate:.1%})")
        return True
        
    except Exception as e:
        print(f"Error writing Cobertura file: {e}")
        return False

def main():
    """Main function."""
    # Default paths
    jacoco_file = "target/site/jacoco/jacoco.xml"
    cobertura_file = "target/site/cobertura/coverage.xml"
    source_dirs = ["src/main/java"]
    
    # Check if JaCoCo file exists
    if not os.path.exists(jacoco_file):
        print(f"JaCoCo file not found: {jacoco_file}")
        print("Please run 'mvn test jacoco:report' first.")
        return 1
    
    # Convert
    success = convert_jacoco_to_cobertura(jacoco_file, cobertura_file, source_dirs)
    return 0 if success else 1

if __name__ == "__main__":
    sys.exit(main())
