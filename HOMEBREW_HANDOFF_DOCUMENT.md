# INGenious Homebrew Guide

## Hosting

The Homebrew setup uses two repositories:

- **Cask repository:** [kiandimla/homebrew-ingenious-poc](https://github.com/kiandimla/homebrew-ingenious-poc)
- **Cask definition:** [Casks/ingenious.rb](https://github.com/kiandimla/homebrew-ingenious-poc/blob/main/Casks/ingenious.rb)
- **Installer releases:** [kiandimla/INGenious releases](https://github.com/kiandimla/INGenious/releases)

The Cask contains the application version, architecture-specific download URLs, SHA-256 checksums, PKG filenames, and uninstall instructions.

Each release hosts separate installers for Apple Silicon and Intel Macs:

```text
INGenious-<version>-macos-arm64.pkg
INGenious-<version>-macos-x86_64.pkg
```

Homebrew automatically detects the Mac architecture and downloads only the matching PKG.

## Install

For a first-time installation from the POC tap:

```zsh
brew tap kiandimla/ingenious-poc
brew trust kiandimla/ingenious-poc
brew install ingenious
```

Alternatively, use the fully qualified package name:

```zsh
brew install kiandimla/ingenious-poc/ingenious
```

## Upgrade

Update Homebrew and the registered tap, then install the latest Cask version:

```zsh
brew update
brew upgrade ingenious
```

For every release, both architecture-specific PKGs must be uploaded and the Cask version and SHA-256 checksums must be updated.

## Uninstall

```zsh
brew uninstall ingenious
```

Uninstalling removes the application and its package receipt. The user Workspace is preserved at:

```text
~/Library/Application Support/INGenious
```
