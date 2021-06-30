#!/bin/bash

echo "Running dlibms in Multi Tenant mode..."
../gradlew bootRun --args='--spring.profiles.active=multi-tenant'


