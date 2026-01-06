# Discord messages llm moderator
Discord messages llm moderator is a Discord's bot for automatic servers messages moderation.
It uses a large language model for analyzing messages if it breaks netiquette.
If a message breaks netiquette, then it deletes it. Currently, the reason for deleting message can be seen only in logs.

## Getting started
### Configure your bot instance on Discord site
Before run, you need to create and configure your bot instance in Discord Developers portal https://discord.com/developers/applications
Then give your bot permissions, which allow viewing and deleting messages.
In the end, install bot on selected Discord server.
For more information, please visit https://discord.com/developers/docs/quick-start/getting-started

### Build and run locally
At first, you need to set environment variable DISCORD_BOT_TOKEN with application's token obtained from the Discord Developers portal.
Then run command:
```
docker compose up -d
```
Take into account that the first run could take some time, because LLM has to be downloaded.

## Example improvements, which can be done
* Consider using other llm, which consumes fewer resources or gives more accurate responses.
* Consider tune prompt for analyzing messages.
* Add checking attachments in messages.
* Add sending notifications to a specified Discord channel for server's moderators about deleted messages.
* Add option for a not deleting message, but only sending notifications through for example email or message to a specified channel on a server.
* Implement multithreading support. Currently, all messages are processed in one thread one by one.
The use of virtual threads should be considered.

### License
The application is licensed under the MIT license.