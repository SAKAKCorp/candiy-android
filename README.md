# HealthConnect Android App

This Android app integrates with the HealthConnect SDK to collect and manage health-related data such as heart rate, step count, calories burned, sleep data, and more.

## Features
- Read and write health data such as heart rate, steps, distance, and calories burned.
- Access health data through the HealthConnect API.
- Display real-time health information from connected devices.
- Store data from HealthConnect API in RoomDB and sync only necessary data to the candiy-pocket server.
- Save connected device information in RoomDB.

## Android Debug Database(RoomDB)
To view and analyze the data, open the Android Debug Database by starting the application. You will see a logcat entry like this:

D/DebugDB: Open http://XXX.XXX.X.XXX:8080 in your browser 

<img width="1503" alt="Screenshot 2025-04-11 at 1 57 42 PM" src="https://github.com/user-attachments/assets/63b91f18-ae85-4e2b-97e5-6b8955ba646e" />

Then, open the shown URL in your browser to access the debug database.
