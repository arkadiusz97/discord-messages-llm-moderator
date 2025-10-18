# Discord messages llm moderator

## Getting started
### Configure your bot instance on Discord site
Before run, you need to create and configure your bot instance in Discord Developers portal.
Then give to application permissions, which allow viewing and deleting messages.
In the end, install bot on selected Discord server.

### Build and run locally
At first, you need to set environment variable DISCORD_BOT_TOKEN with application token obtained from the Discord Developers portal.
Then run command:
```
docker compose up -d
```
Take into account, that the first run could take some time, because LLM has to be downloaded.

## Example improvements, which can be done
* Consider using other llm, which consumes fewer resources or gives more accurate responses.
* Consider tune prompt for analyzing messages.
* Resolve a problem with pull-model-strategy: always.
* Add checking attachments in messages.
* Add sending notifications to a specified Discord channel for server's moderators about deleted messages.
* Add option for a not deleting message, but only sending notifications.

### License
The application is licensed under the MIT license.