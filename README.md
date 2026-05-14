<div align="center">

# Hypertube

### 42 Madrid — Outer Core Project

![42 School](https://img.shields.io/badge/42-Madrid-000000?style=for-the-badge&logo=42&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Angular](https://img.shields.io/badge/Angular-DD0031?style=for-the-badge&logo=angular&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [API](#api)
- [Team](#team)

---

## Overview

Hypertube is a 42 Common Core project. The goal is to build a complete video streaming platform from scratch — users can search for movies, stream them directly in the browser via BitTorrent, and browse a rich catalog powered by TMDB metadata.

The platform handles torrent downloading and streaming on the fly, serving video content to the browser while it is still being downloaded. Users can authenticate via local accounts, Google OAuth, or 42 OAuth, browse movie catalogs, read comments, and watch films with subtitles sourced from OpenSubtitles.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Angular, TypeScript, CSS |
| Backend | Java, Spring Boot |
| Database | PostgreSQL |
| Auth | JWT, Google OAuth 2.0, 42 OAuth 2.0 |
| Streaming | BitTorrent (TCP/UDP 6891–6991) |
| Metadata | TMDB API |
| Subtitles | OpenSubtitles API |
| Containerization | Docker, Docker Compose |

---

## Architecture

Everything runs inside Docker and starts with a single command.

```
                    [ Browser ]
                         |
                       HTTP
                         |
              +----------+----------+
              |                     |
    +---------+--------+ +-----------+----------+
    |     Frontend     | |        Backend        |
    |  Angular 4200    | |   Spring Boot 8080    |
    |  TypeScript      | |   REST API  /  JWT    |
    +------------------+ +-----------+-----------+
                                     |
                         +-----------+-----------+
                         |       PostgreSQL       |
                         |    (port 5432)         |
                         +-----------------------+

    +--------------------------------------------------+
    |              BitTorrent Engine                   |
    |   TCP/UDP ports 6891–6991 — stream-on-download   |
    +--------------------------------------------------+

    +--------------------------------------------------+
    |     External APIs: TMDB · OpenSubtitles          |
    +--------------------------------------------------+
```

The Angular SPA communicates with the Spring Boot REST API. The backend manages torrent sessions, downloading and serving video chunks to the client in real time. Movie metadata and posters are fetched from TMDB. Subtitles are retrieved from OpenSubtitles and served alongside the stream. Authentication is handled via JWT with support for local accounts, Google OAuth 2.0, and 42 OAuth 2.0.

---

## Getting Started

**Requirements**

- Docker >= 24.x
- Docker Compose >= 2.x
- make

**Steps**

```bash
git clone git@github.com:Ja1m3st/Hypertube.git
cd Hypertube
make
```

Open [http://localhost:4000](http://localhost:4000) in your browser.

**Makefile targets**

| Command | Description |
|---|---|
| `make` | Build and start all containers |
| `make up` | Start without rebuilding |
| `make down` | Stop and remove containers |
| `make clean` | Stop containers and remove volumes |
| `make fclean` | Remove containers, volumes, and images |
| `make logs` | Tail logs from all services |

---

## Environment Variables

A `.env` file is required at the root level.

> Sensitive values are marked as `your_*`. Replace them before running the project.

---

### `.env` — root level

Used by the backend and database containers.

```env
# ── Database ──────────────────────────────────────────────────────
DB_NAME=your_db_name
DB_USER=your_db_user
DB_PASSWORD=your_db_password

# ── Auth ──────────────────────────────────────────────────────────
JWT_KEY=your_jwt_secret_key

# ── Google OAuth 2.0 ──────────────────────────────────────────────
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# ── 42 OAuth 2.0 ──────────────────────────────────────────────────
FORTYTWO_CLIENT_ID=your_42_client_id
FORTYTWO_CLIENT_SECRET=your_42_client_secret

# ── External APIs ─────────────────────────────────────────────────
TMDB_KEY=your_tmdb_api_key
API_OPENSUBTITLES=your_opensubtitles_api_key
```

**Service URLs once running**

| Service | URL |
|---|---|
| Application | http://localhost:4000 |
| Backend API | http://localhost:8080 |
| Database    | localhost:5432        |

---

## API

The backend exposes a REST API under `/api`. All routes that require authentication expect a valid JWT in the `Authorization` header.

| Group | Routes |
|---|---|
| `/api/auth` | local registration, login, Google OAuth callback, 42 OAuth callback |
| `/api/users` | profile, avatar, password update, user search |
| `/api/movies` | search catalog, movie details, TMDB metadata |
| `/api/stream` | torrent download, video streaming, subtitle delivery |
| `/api/comments` | post and fetch comments per movie |

---

## Team

| Name | GitHub |
|---|---|
| Jaime Sanchez | [Ja1m3st](https://github.com/Ja1m3st) |

---

<div align="center">
42 Madrid
</div>
