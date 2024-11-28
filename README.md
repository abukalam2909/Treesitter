# Table of Contents
1. [Introduction](#introduction)
2. [Dependencies](#dependencies)
3. [Build/deployment Instructions](build/deployment-instructions)
4. [Usage Scenarios](#usage-scenarios)
   - [Command Line Options](#command-line-options)
   - [Supported Refactoring Types](#supported-refactoring-types)
   - [Output Format Example](#output-format)
5. [Test-Driven Development (TDD)](#test-driven-development-tdd)
   - [Initial Refactoring Detection Rules and Python Support](#1-initial-refactoring-detection-rules-and-python-support)
   - [Extension to Additional Languages](#2-extension-to-additional-languages)
   - [Development Approach Explanation](#development-approach-explanation)

# Usage Scenarios

## Command Line Options

The tool supports three main command-line options:

* `-a`: Analyze all commits in a local repository branch
* `-c`: Analyze a specific commit in a local repository
* `-gc`: Analyze a specific commit from a GitHub repository

### 1. Analyzing All Commits in a Local Repository
```
./treefactor.sh -a <path-to-local-repo> <branch-name>
```
This command detects refactorings for all commits in the specified branch <branch-name> of a local repository. The tool will:
- Scan through the entire commit history
- Detect refactorings in Python, JavaScript, and C++ files
- Output detected refactorings for each commit

If no branch is specified, it will analyze commits from all branches:
```
./treefactor.sh -a <path-to-local-repo>
```
### 2. Analyzing a Specific Commit in a Local Repository
```
./treefactor.sh -c <path-to-local-repo> <commit-hash>
```
This command detects refactorings at a specific commit <commit-hash> of a local repository. Useful when you want to:
- Check refactorings in a particular change
- Validate refactoring operations before merging
- Review historical changes

### 3. Analyzing a GitHub Repository Commit
```
./treefactor.sh -gc <git-url> <token> <commit-hash> <timeout>
```
This command detects refactorings at a specified commit <commit-hash> for project <git-url> within the given <timeout> in seconds. It requires a GitHub authentication token.



## Supported Refactoring Types
### Python
The tool provides four types of refactoring detection for Python code, including:
#### 1. Parameter Renaming
```
# Before
def greet(msg):
    print(msg)
    
# After
def greet(message):
    print(message)
```
In this example, parameter is renamed from *msg* to *message*.

#### 2. Parameter Addition
```
# Before
def greet(name):
    print(f"Hello, {name}!")
    
# After
def greet(name, greeting="Hello"):
    print(f"{greeting}, {name}!")
```
In this example, a new parameter with the name *greeting* is added.

#### 3. Parameter Type Changes
```
# Before
def process(data: list):
    pass
    
# After
def process(data: List[str]):
    pass
```
In this example, the type of the parameter is changed from list to List[str].

#### 4. Method Renaming
```
# Before
def calc_sum(numbers):
    return sum(numbers)
    
# After
def calculate_sum(numbers):
    return sum(numbers)
```
In this example, the method is renamed from *calc_sum* to *calculate_sum*.

### JavaScript 
For JavaScript, the tool currently supports parameter renaming detection.
```
// Before
function calculate(n) {
    return n * 2;
}

// After
function calculate(num) {
    return num * 2;
}
```
In this example, parameter is renamed from *n* to *num*.


### C++ 
For C++, the tool currently supports parameter renaming detection.
```
// Before
void process(int x) {
    std::cout << x << std::endl;
}

// After
void process(int value) {
    std::cout << value << std::endl;
}
```
In this example, parameter is renamed from *x* to *value*.

## Output Format
As shown in the real output, the tool provides detailed information about each refactoring:
```
Commit ID: XXXXXX
Commit Message: refactor
Parent Commit ID: XXXXXX
Refactorings:
        Rename Parameter: n renamed to name in greet
        Add Parameter in Python: Parameter 'tax_rate' of type 'object' added to function 'calculate_total'
```
The output includes:
- Complete commit hash
- Commit message
- Parent commit ID for reference
- List of detected refactorings with specific details about each change


# Test-Driven Development (TDD)
This project follows Test-Driven Development with the following implementation order:

## 1. Initial Refactoring Detection Rules and Python Support
We first developed the core refactoring detection functionality with Python as the initial supported language.

### Python AST Visitor Implementation
1. Initial Development:
   - First Test: [270cb37](https://github.com/CSCI5308/course-project-g03/commit/270cb37) - Added test for PythonASTVisitor
   - Implementation: [e8698f7](https://github.com/CSCI5308/course-project-g03/commit/e8698f7) - Updated PythonASTVisitor

2. Keyword Parameters Feature:
   - Test: [1e2fd3d](https://github.com/CSCI5308/course-project-g03/commit/1e2fd3dd654edacda2e2f1c08d98b2e6d707a009) - Added tests for KeywordOnlyParameters
   - Implementation: [211efac](https://github.com/CSCI5308/course-project-g03/commit/211efacaa586abb42ac0905d89e5d044e371298b) - Implemented keyword-only parameters detection

### UML Model and Refactoring Detection
1. Test Creation: [63ac58e](https://github.com/CSCI5308/course-project-g03/commit/63ac58e214c6bfe832b43000c844c72685c690f2) - UMLModelTest
2. Implementation: [0c1230d](https://github.com/CSCI5308/course-project-g03/commit/0c1230da1cbae6796d2143c1e86870a0a353b52b) - UMLModel and Refactoring Detection Implementation

## 2. Extension to Additional Languages
After establishing the core refactoring detection rules with Python, we extended support to C++ and JavaScript.

### C++ Support
1. AST Visitor Development:
   - Test: [b885569](https://github.com/CSCI5308/course-project-g03/commit/b885569140edabd999f69ccc1ed018a7b1c91111) - Added CPPASTVisitor tests
   - Implementation: [c99dbf9](https://github.com/CSCI5308/course-project-g03/commit/c99dbf9569060598bc568b6304d751b53216b597) - Implemented CPPASTVisitor
   - Test Validation: [92ee414](https://github.com/CSCI5308/course-project-g03/commit/92ee414a570aa86855389d252a6219ef8fff93ca) - All tests passing
  
2. Refactoring Detection Test:
   - Test: [b8b9768](https://github.com/CSCI5308/course-project-g03/commit/b8b9768) - Created CppRenameParameterTest

### JavaScript Support

1. AST Visitor Development:
   - Test: [28374ed](https://github.com/CSCI5308/course-project-g03/commit/28374ed1a01938f1e4841efe6c81ab49d554e25a) - Added JSASTVisitor tests
   - Implementation: [d055021](https://github.com/CSCI5308/course-project-g03/commit/d055021826d52e329c9b4e892c181aa343c98e81) - Implemented JSASTVisitor
2. Refactoring Detection Test:
   - Test: [5603ae0](https://github.com/CSCI5308/course-project-g03/commit/5603ae0) - Created JSRenameParameterTest

## Development Approach Explanation
1. We first developed and tested the core refactoring detection rules using Python as our initial language. 

2. Once the core rules were working with Python, we created specific tests for C++ and JavaScript (CppRenameParameterTest and JSRenameParameterTest) to verify that our refactoring detection rules could be applied to these languages.

3. For each language, we followed TDD by:
   - First creating tests for language-specific AST visitors
   - Implementing the visitors
   - Validating that both the visitor tests and refactoring detection tests passed
