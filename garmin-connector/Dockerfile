# syntax=docker/dockerfile:1.7
FROM python:3.12.11-slim-bookworm

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

RUN groupadd --system --gid 10001 pace \
    && useradd --system --uid 10001 --gid pace --home-dir /nonexistent --shell /usr/sbin/nologin pace

WORKDIR /application
COPY pyproject.toml requirements.lock README.md ./
COPY src ./src
RUN pip install --requirement requirements.lock \
    && pip install --no-deps .

RUN mkdir /donnees && chown pace:pace /donnees
USER 10001:10001
EXPOSE 8081
CMD ["uvicorn", "pace_garmin.main:app", "--host", "0.0.0.0", "--port", "8081", "--no-access-log"]
