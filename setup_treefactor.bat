@echo off
REM Check if OS is Windows
setlocal
set "OS_NAME=windows"
set "EXT=dll"

REM Set paths
set "ROOT_DIR=%cd%"
set "NATIVE_DIR=%ROOT_DIR%\src\main\resources\native\%OS_NAME%"
if not exist "%NATIVE_DIR%" mkdir "%NATIVE_DIR%"

REM Function to clone, compile, and move library files
setlocal enabledelayedexpansion
:install_parser
    set "REPO_URL=%1"
    set "LIB_NAME=%2"
    set "REPO_DIR=%LIB_NAME%-repo"

    echo Setting up %LIB_NAME%...

    REM Clone the repository
    git clone %REPO_URL% %REPO_DIR%
    cd %REPO_DIR%

    REM Compile the .c files into the appropriate shared library
    gcc -shared -I./src -o lib%LIB_NAME%.%EXT% src\parser.c src\scanner.c

    REM Move the compiled library to the native directory
    move lib%LIB_NAME%.%EXT% "%NATIVE_DIR%"

    REM Clean up
    cd ..
    rmdir /S /Q %REPO_DIR%
    echo %LIB_NAME% setup complete!

endlocal & goto:eof

REM Install the core tree-sitter library
echo Setting up core Tree-sitter library...
git clone https://github.com/tree-sitter/tree-sitter.git tree-sitter
cd tree-sitter
make
move libtree-sitter.%EXT% "%ROOT_DIR%"
cd ..
rmdir /S /Q tree-sitter
echo Core Tree-sitter setup complete!

REM Install each tree-sitter parser
call :install_parser https://github.com/tree-sitter/tree-sitter-python.git tree-sitter-python
call :install_parser https://github.com/tree-sitter/tree-sitter-cpp.git tree-sitter-cpp
call :install_parser https://github.com/tree-sitter/tree-sitter-javascript.git tree-sitter-javascript

echo All libraries have been set up for %OS_NAME%!
endlocal
pause
