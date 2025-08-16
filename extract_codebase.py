#!/usr/bin/env python3
"""
Android Studio Project Codebase Extractor
Generates a comprehensive .txt file containing the entire project structure and source code.
"""

import os
import datetime
from pathlib import Path

def should_include_file(file_path, output_filename="codebase_export.txt"):
    """
    Determine if a file should be included in the codebase export.
    """
    # Include these file extensions (TEXT FILES ONLY)
    include_extensions = {
        '.java', '.kt', '.xml', '.gradle', '.properties', '.json',
        '.md', '.txt', '.yml', '.yaml', '.pro', '.gitignore'
        # Removed image files: .png, .jpg, .jpeg, .webp, .svg, .9.png
        # Images are binary and create garbage text when read
    }

    # PREVENT RECURSION: Exclude the output file itself!
    file_name = Path(file_path).name
    if file_name == output_filename or file_name.endswith("_codebase_export.txt"):
        return False

    # Exclude these directories
    exclude_dirs = {
        'build', '.gradle', '.idea', 'captures', '.externalNativeBuild',
        '.cxx', 'node_modules', 'venv', '__pycache__', '.git', '.kotlin'
    }

    # Check if any parent directory should be excluded
    path_parts = Path(file_path).parts
    if any(excluded in path_parts for excluded in exclude_dirs):
        return False

    # Check file extension
    return Path(file_path).suffix.lower() in include_extensions

def generate_tree_structure(root_dir, prefix="", max_depth=10, current_depth=0):
    """
    Generate a tree-like structure of the project directory.
    """
    if current_depth > max_depth:
        return ""

    tree_output = ""
    root_path = Path(root_dir)

    try:
        # Get all items and sort them (directories first, then files)
        items = list(root_path.iterdir())
        items.sort(key=lambda x: (x.is_file(), x.name.lower()))

        for i, item in enumerate(items):
            # Skip excluded directories and files
            if not should_include_file(str(item)) and item.is_file():
                continue
            if item.is_dir() and any(excluded in item.name for excluded in
                                     ['build', '.gradle', '.idea', 'captures', '.externalNativeBuild', '.cxx']):
                continue

            # Determine if this is the last item
            is_last = i == len(items) - 1

            # Choose the appropriate tree characters
            current_prefix = "└── " if is_last else "├── "
            tree_output += f"{prefix}{current_prefix}{item.name}\n"

            # If it's a directory, recurse into it
            if item.is_dir():
                extension_prefix = "    " if is_last else "│   "
                tree_output += generate_tree_structure(
                    item,
                    prefix + extension_prefix,
                    max_depth,
                    current_depth + 1
                )

    except PermissionError:
        tree_output += f"{prefix}└── [Permission Denied]\n"

    return tree_output

def extract_file_content(file_path):
    """
    Extract and return the content of a file with proper encoding handling.
    """
    file_ext = Path(file_path).suffix.lower()

    # Skip binary files (images, etc.)
    binary_extensions = {'.png', '.jpg', '.jpeg', '.webp', '.gif', '.bmp', '.ico', '.9.png'}
    if file_ext in binary_extensions:
        file_size = os.path.getsize(file_path)
        return f"[BINARY FILE - {file_ext.upper()} - Size: {file_size} bytes]"

    try:
        # Try UTF-8 first
        with open(file_path, 'r', encoding='utf-8') as file:
            return file.read()
    except UnicodeDecodeError:
        try:
            # Try with latin-1 as fallback
            with open(file_path, 'r', encoding='latin-1') as file:
                return file.read()
        except Exception as e:
            return f"[Error reading file: {str(e)}]"
    except Exception as e:
        return f"[Error reading file: {str(e)}]"

def get_project_files(root_dir, output_file="codebase_export.txt"):
    """
    Get all relevant project files recursively.
    """
    project_files = []
    root_path = Path(root_dir)

    for file_path in root_path.rglob('*'):
        if file_path.is_file() and should_include_file(str(file_path), os.path.basename(output_file)):
            # Get relative path from project root
            relative_path = file_path.relative_to(root_path)
            project_files.append((str(relative_path), str(file_path)))

    # Sort files by path for consistent ordering
    project_files.sort(key=lambda x: x[0].lower())
    return project_files

def generate_codebase_export(project_root, output_file="codebase_export.txt"):
    """
    Generate the complete codebase export file.
    """
    project_path = Path(project_root)
    if not project_path.exists():
        print(f"Error: Project directory '{project_root}' does not exist!")
        return

    project_name = project_path.name
    current_time = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    print("Starting codebase extraction...")
    print(f"Project: {project_name}")
    print(f"Root: {project_root}")

    with open(output_file, 'w', encoding='utf-8') as output:
        # Write header
        output.write("=" * 80 + "\n")
        output.write(f"ANDROID PROJECT CODEBASE EXPORT\n")
        output.write("=" * 80 + "\n")
        output.write(f"Project Name: {project_name}\n")
        output.write(f"Export Date: {current_time}\n")
        output.write(f"Root Directory: {project_root}\n")
        output.write("=" * 80 + "\n\n")

        # Generate and write project structure tree
        output.write("PROJECT STRUCTURE\n")
        output.write("-" * 40 + "\n")
        output.write(f"{project_name}/\n")
        tree_structure = generate_tree_structure(project_root)
        output.write(tree_structure)
        output.write("\n\n")

        # Write file contents
        output.write("FILE CONTENTS\n")
        output.write("-" * 40 + "\n\n")

        project_files = get_project_files(project_root, output_file)
        total_files = len(project_files)

        for i, (relative_path, full_path) in enumerate(project_files, 1):
            print(f"Processing ({i}/{total_files}): {relative_path}")

            # Write file header
            output.write("=" * 60 + "\n")
            output.write(f"FILE: {relative_path}\n")
            output.write("=" * 60 + "\n")

            # Write file content
            content = extract_file_content(full_path)
            output.write(content)
            output.write("\n\n")

        # Write footer
        output.write("=" * 80 + "\n")
        output.write(f"EXPORT COMPLETED - {total_files} files processed\n")
        output.write(f"Generated: {current_time}\n")
        output.write("=" * 80 + "\n")

    print(f"\nCodebase export completed!")
    print(f"Output file: {output_file}")
    print(f"Total files processed: {total_files}")

if __name__ == "__main__":
    # Configuration
    PROJECT_ROOT = r"C:\ErlavushGitHubFiles\ejTouch"  # Update this path
    OUTPUT_FILE = "ejTouch_codebase_export.txt"

    print("Android Project Codebase Extractor")
    print("=" * 50)

    # You can modify the project root here or make it interactive
    user_input = input(f"Press Enter to use default path ({PROJECT_ROOT}) or enter new path: ").strip()
    if user_input:
        PROJECT_ROOT = user_input

    generate_codebase_export(PROJECT_ROOT, OUTPUT_FILE)