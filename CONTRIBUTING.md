# Contributing to INGenious

First of all, thank you for considering contributing to INGenious! We appreciate your time and effort to make this project even better. Below are guidelines to help you get started.

## How Can You Contribute?

There are many ways to contribute to INGenious:

1. *Reporting Bugs:* Found a bug? Help us improve by submitting an issue.
2. *Proposing Features:* Got an idea to enhance INGenious? We’re all ears!
3. *Fixing Issues:* You can pick up existing issues to work on.
4. *Writing Documentation:* Good documentation is vital, and your help is welcome.
5. *Improving Test Coverage:* Help us by writing additional tests.

## Getting Started

### 1. Fork and Clone the Repository
First, fork the repository and then clone it to your local machine:
```bash
git clone https://github.com/ing-bank/INGenious.git
```

Navigate to the project directory:
```bash
cd INGenious
```

### 2. Install Dependencies
Install the necessary dependencies using:
```bash
mvn install
```

### 3. Create a Branch
Always create a new branch for your contributions:
```bash
git checkout -b your-branch-name
```

Use descriptive names for branches like feature/add-new-feature or bugfix/fix-issue.

### 4. Make Your Changes
Make your changes in the relevant files. Be sure to follow the coding standards and practices already in place.

### 5. Test Your Changes
Ensure that your changes do not break any existing features:
```bash
mvn test
```

You can also perform `mvn clean install` which will build the INGenious final package in the `Dist/release` directory. 
You can open INGenious from there as well if you want to test the changes from the framework.

### 6. Commit and Push Your Changes
Commit your changes with a clear and concise message:
```bash
git commit -m "Add a brief description of your changes"
```

Push your changes:
```bash
git push origin your-branch-name
```


### 7. Open a Pull Request
Go to the original repository on GitHub and open a Pull Request (PR) against the main branch. Please provide the following details:

- *Title:* A clear and descriptive title.
- *Description:* Explain what changes you’ve made, why they’re necessary, and any relevant details.


## Contribution Guidelines

- Ensure your code adheres to the existing style guides and best practices.
- Keep your pull requests focused on a single issue or feature; avoid large, mixed changes.
- Update documentation if your contribution impacts usage or configuration.
- Write clear, descriptive commit messages.

## Reporting Issues

If you encounter a bug or would like to request a feature, please check [existing issues](https://github.com/ing-bank/INGenious/issues) first to avoid duplicates. If none match, feel free to open a new one with the following details:

1. *Title:* Short and specific description.
2. *Description:* Detailed information about the issue or request.
3. *Steps to Reproduce (for bugs):* Provide a clear path to replicate the problem.
4. *Environment:* Mention relevant details like OS, Node.js version, etc.


## Questions or Help

If you have any questions or need help, feel free to reach out by opening an issue or joining the discussion [here](https://github.com/ing-bank/INGenious/discussions).

---

Thank you for contributing and helping us make INGenious better!

---