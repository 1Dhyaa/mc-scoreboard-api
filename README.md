# mc-scoreboard-api

A lightweight REST API for managing Minecraft server scoreboards and player statistics.

Built this to handle leaderboards and player tracking across multiple game modes on my server.

## Features

- Player score tracking (kills, deaths, wins, playtime)
- Leaderboard endpoints with pagination
- Per-game-mode statistics
- SQLite storage (zero config)

## Endpoints

```
GET    /api/players              - list all players
GET    /api/players/:uuid        - get player stats
POST   /api/players/:uuid/score  - update a score
GET    /api/leaderboard/:mode    - top players for a game mode
DELETE /api/players/:uuid        - remove a player
```

## Setup

```bash
# clone it
git clone https://github.com/1Dhyaa/mc-scoreboard-api.git
cd mc-scoreboard-api

# compile
javac -cp "lib/*" -d out src/**/*.java

# run
java -cp "out:lib/*" com.dhyaa.scoreboard.Main
```

Server starts on port `8080` by default. Change it in `config.properties`.

## Example

```bash
# get leaderboard for bedwars
curl http://localhost:8080/api/leaderboard/bedwars?limit=10

# update player score
curl -X POST http://localhost:8080/api/players/some-uuid/score \
  -H "Content-Type: application/json" \
  -d '{"mode": "bedwars", "kills": 3, "deaths": 1, "won": true}'
```

## Tech

- Java 17
- Built-in HTTP server (com.sun.net.httpserver)
- SQLite via JDBC
- No frameworks, no bloat

## License

MIT
