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


