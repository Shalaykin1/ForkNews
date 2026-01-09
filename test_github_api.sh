#!/bin/bash
echo "Testing GitHub API..."
echo ""
echo "1. Testing /releases endpoint (all releases):"
curl -s "https://api.github.com/repos/K11MCH1/AdrenoToolsDrivers/releases" | head -20
echo ""
echo ""
echo "2. Testing /releases/latest endpoint:"
curl -s "https://api.github.com/repos/K11MCH1/AdrenoToolsDrivers/releases/latest" | head -20
