# ViaUpdater

A Paper plugin that keeps your ViaVersion plugins up to date automatically.
It supports pulling builds from Jenkins CI servers and from GitHub Actions artifacts or source code.
Projects are fully configurable through a YAML config file.

## Features

- Update ViaVersion, ViaBackwards, ViaRewind, and any other configurable project
- Two source types: Jenkins CI and GitHub (source build or workflow artifact)
- Per-project source selection with a named default
- Auto-update scheduler with a configurable interval
- In-game command with tab completion
- Isolated Gradle and Maven cache inside the tmp folder during source builds (configurable)

## Installation

1. Build the project with `./gradlew build`
2. Drop the jar into your server's `plugins` folder
3. Start the server once to generate `plugins/ViaUpdater/config.yml`
4. Fill in your GitHub token and adjust the project list as needed
5. Restart or reload the config with `/viaupdater reload`

## Configuration

```yaml
# Whether to delete build data automatically after completion
cleanup: false

# Redirect GRADLE_USER_HOME and Maven local repository into the tmp folder during source builds
# Keeps the global Gradle/Maven cache untouched
isolated-cache: true

github:
  # Personal Access Token used to authenticate with GitHub API
  # Required to avoid rate limits and access private repositories if needed
  # How to get it:
  # 1. Go to: https://github.com/settings/tokens/new
  # 2. Generate a new token (classic)
  # 3. Select scope: "repo" (Full control of private repositories)
  # 4. Copy the token and paste it here
  token: <GITHUB-TOKEN>

jenkins:
  # Base URL of the ViaVersion Jenkins instance
  endpoint: https://ci.viaversion.com

auto-update:
  # Enable or disable automatic update checks
  enabled: false

  # How often to check for updates (in hours)
  interval: 24

projects:
  - name: ViaVersion

    # Default source to use when no specific source is requested
    default: jenkins

    sources:
      # Main GitHub repository (stable releases)
      - id: github
        type: GITHUB
        owner: ViaVersion
        repository: ViaVersion
        branch: master
        # If they add artifacts back to the public repository we could download them directly from GitHub
        #workflow: build.yml

      # Development / preview builds (sponsor-only repo)
      - id: github-sponsor
        type: GITHUB
        owner: ViaVersion
        repository: ViaVersionDev
        branch: preview

      # Jenkins CI builds
      - id: jenkins
        type: JENKINS
        project: ViaVersion
```

### Source types

**JENKINS**

| Field     | Description                        |
|-----------|------------------------------------|
| `id`      | Unique identifier for this source  |
| `type`    | `JENKINS`                          |
| `project` | Project name on the Jenkins server |

**GITHUB (artifact download)**

Set the `workflow` field to download the artifact produced by a workflow run instead of building from source.

| Field      | Description                               |
|------------|-------------------------------------------|
| `id`       | Unique identifier for this source         |
| `type`     | `GITHUB`                                  |
| `owner`    | Repository owner                          |
| `repository` | Repository name                         |
| `branch`   | Branch to use                             |
| `workflow` | Workflow file name (e.g. `build.yml`)     |

**GITHUB (source build)**

When `workflow` is omitted the plugin downloads the source zip and builds the project locally using the bundled Gradle wrapper.

## Commands

Permission: `viaupdater.command`

| Command                                   | Description                                          |
|-------------------------------------------|------------------------------------------------------|
| `/viaupdater update all`                  | Update all installed plugins using their default source |
| `/viaupdater update <name>`               | Update a single plugin using its default source      |
| `/viaupdater update <name> <source>`      | Update a single plugin using a specific source       |
| `/viaupdater reload`                      | Reload the config without restarting the server      |

Aliases: `viaupdate`, `updatevia`, `updateviaversion`
