# Discord messages llm moderator
Discord messages llm moderator is a Discord's bot for automatic servers messages moderation and/or for alert server staff about messages, which could require moderation.
It uses a large language model(llm) for analyzing messages if it breaks netiquette.
If a message breaks netiquette, then it deletes it and/or sends notifications through email about those messages.

## Getting started
### Configure your bot instance on Discord site
Before run, you need to create and configure your bot instance in Discord Developers portal https://discord.com/developers/applications
Then give your bot permissions, which allow viewing and deleting messages.
In the end, install bot on selected Discord server.
For more information, please visit https://discord.com/developers/docs/quick-start/getting-started

### Build and run locally
At first, you need to set the following mandatory environment variables:
* **DISCORD_BOT_TOKEN** - token obtained from the Discord Developers portal.
* **DISCORD_BOT_MAIL_HOST** - mail from which notifications are being sent.
* **DISCORD_BOT_MAIL_PORT** - port for mail server from which notifications are being sent.
* **DISCORD_BOT_MAIL_USERNAME** - mail address from which notifications are being sent.
* **DISCORD_BOT_MAIL_PASSWORD** - password for mail address from which notifications are being sent.
* **DISCORD_BOT_MAIL_TO_ADDRESS** - mail address to which notifications are being sent.
Then run command:
```
docker compose up -d
```
Take into account that the first run could take some time, because LLM has to be downloaded.

## Example improvements, which can be done
* Use Dead Letter Exchange with delayed and limited retries for queue in case of negative acknowledge.
* Put analyzed messages by LLM to another queue to not send this message again to LLM,
when negative acknowledge is sent due to Discord's API error during a deleting message.
* Consider using other llm, which consumes fewer resources or gives more accurate responses.
* Consider tune prompt for analyzing messages.
* Add checking attachments in messages.
* Add bot commands to run on server to change bot configuration and persist it in a database.
* Add sending notifications about messages to a selected channel on Discord server.

### License
The application is licensed under the MIT license.