#!/usr/bin/env bash
curl -X POST http://localhost:8080/questionanswering -d @batman.json  | python -m json.tool
