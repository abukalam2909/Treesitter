#!/bin/bash

# Detect the operating system and set the library file extension accordingly
OS=$(uname)
case "$OS" in
    Darwin)
        EXT="dylib"
        OSname="macos"
        MAKE_CMD="make"
        ;;
    Linux)
        EXT="so"
        OSname="linux"
        MAKE_CMD="make"
        ;;
    CYGWIN*|MINGW32*|MSYS*|MINGW*)
        EXT="dll"
        OSname="windows"
        MAKE_CMD="gcc"  # Use gcc directly for Windows
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

    # For Windows, use gcc directly for compiling the shared library
    if [ "$OSname" == "windows" ]; then
        # Adjust this command to match the specific file paths on your system
        gcc -o "lib${LIB_NAME}.${EXT}" -shared -I./src -I./include src/parser.c src/scanner.c
    else
        # For Linux/macOS, use make
        gcc -shared -I./src -o "lib${LIB_NAME}.${EXT}" src/parser.c src/scanner.c
    fi

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

# For Windows, use gcc directly
if [ "$OSname" == "windows" ]; then
    gcc -o tree-sitter.dll -shared -I lib/src -I lib/include lib/src/lib.c
else
    $MAKE_CMD  # Use make on Linux/macOS
fi

mv "tree-sitter.${EXT}" "$ROOT_DIR"
cd ..
rm -rf tree-sitter
echo "Core Tree-sitter setup complete!"

# Install each tree-sitter parser
install_parser "https://github.com/tree-sitter/tree-sitter-python.git" "tree-sitter-python"
install_parser "https://github.com/tree-sitter/tree-sitter-cpp.git" "tree-sitter-cpp"
install_parser "https://github.com/tree-sitter/tree-sitter-javascript.git" "tree-sitter-javascript"

echo "All libraries have been set up for $OSname!"
