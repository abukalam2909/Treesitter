#!/bin/bash

# Detect the operating system and set the library file extension accordingly
OS=$(uname)
case "$OS" in
    Darwin)
        EXT="dylib"
        OSname="macos"
        ;;
    Linux)
        EXT="so"
        OSname="linux"
        ;;
    CYGWIN*|MINGW32*|MSYS*|MINGW*)
        EXT="dll"
        OSname="windows"
        ;;
    *)
        echo "Unsupported OS: $OS"
        exit 1
        ;;
esac

echo "Detected OS: $OS, using extension: .$EXT"

# Define project root and native directory
ROOT_DIR=$(pwd)
NATIVE_DIR="${ROOT_DIR}/src/main/resources/native/$OSname"

# Create the native directory if it doesn't exist
mkdir -p "$NATIVE_DIR"

# Clone, compile, and copy library files
install_parser() {
    REPO_URL=$1
    LIB_NAME=$2

    echo "Setting up $LIB_NAME..."

    # Clone the repository
    git clone "$REPO_URL"
    REPO_DIR=$(basename "$REPO_URL" .git)
    cd "$REPO_DIR"

    # Compile the .c files into the appropriate shared library
    gcc -shared -I./src -o "lib${LIB_NAME}.${EXT}" src/parser.c src/scanner.c

    # Move the compiled library to the native directory
    mv "lib${LIB_NAME}.${EXT}" "$NATIVE_DIR"

    # Clean up
    cd ..
    rm -rf "$REPO_DIR"
    echo "$LIB_NAME setup complete!"
}

# Install the core tree-sitter library
echo "Setting up core Tree-sitter library..."
git clone https://github.com/tree-sitter/tree-sitter.git
cd tree-sitter
make
mv "libtree-sitter.${EXT}" "$ROOT_DIR"
cd ..
rm -rf tree-sitter
echo "Core Tree-sitter setup complete!"

# Install each tree-sitter parser
install_parser "https://github.com/tree-sitter/tree-sitter-python.git" "tree-sitter-python"
install_parser "https://github.com/tree-sitter/tree-sitter-cpp.git" "tree-sitter-cpp"
install_parser "https://github.com/tree-sitter/tree-sitter-javascript.git" "tree-sitter-javascript"

echo "All libraries have been set up for $OSname!"
