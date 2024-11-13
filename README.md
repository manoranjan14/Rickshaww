## Rickshaww - Ride-Hailing App Overview

Rickshaww is a user-friendly ride-hailing application designed to connect drivers and riders efficiently. It incorporates various features that enhance the user experience for both parties involved in the ride-sharing process.

## Features

- **User Authentication**: Secure login and registration for both drivers and riders using Firebase Authentication.
- **Real-Time Mapping**: Integrated OSMDroid for real-time location tracking and mapping features.
- **Ride Management**: Drivers can view available rides, accept requests, and navigate to pickup and drop locations.
- **Seamless Navigation**: Provides visual navigation from the current location to the drop-off point.
- **Push Notifications**: Alerts drivers about ride requests and important updates.
- **User-Friendly Interface**: Designed for easy navigation and usability for both drivers and riders.

## Technologies Used

| Technology          | Description                                           |
|---------------------|-------------------------------------------------------|
| Android Development  | Java                                                |
| Backend              | Firebase Authentication, Database                     |
| Mapping              | OSMDroid                                             |
| Design               | XML for layouts, Material Design components           |
| Version Control      | Git                                                  |

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/manoranjan14/Rickshaww.git
   ```
2. Open the project in Android Studio.
3. Install the necessary dependencies and SDKs.
4. Configure Firebase by following the [Firebase setup guide](https://firebase.google.com/docs/android/setup).
5. Run the application on an emulator or physical device.

## Usage

1. **Login/Registration**:
   - Open the app and either log in or register a new account.
   - For drivers, fill in required details such as name, phone number, and vehicle information.

2. **Accept Rides**:
   - After logging in, drivers will see available ride requests.
   - Click on a ride request to accept it and navigate to the pickup location.

3. **Navigate**:
   - The app will provide directions to both pickup and drop locations.

4. **Ride Management**:
   - View ride details and manage rides from the dashboard.

## App Icon

To add an icon to the app, use one of the following methods:

- **Image Asset**:
  1. Right-click on the `res` folder in Android Studio.
  2. Select `New > Image Asset`.
  3. Choose your image and follow prompts to generate icons for different resolutions.

- **Vector Asset**:
  1. Right-click on the `res` folder.
  2. Select `New > Vector Asset`.
  3. Choose from Material Icons or upload your own SVG file.

## Changing App Name

To change the app name:

1. Open `AndroidManifest.xml`.
2. Locate the `android:label` attribute in the `<application>` tag and change its value to your desired app name.
3. Update the name in `res/values/strings.xml`:
   ```xml
   <string name="app_name">Your New App Name</string>
   ```

## Contributing

If you would like to contribute to Rickshaww's development, follow these steps:

1. Fork the repository.
2. Create a new branch:
   ```bash
   git checkout -b feature/YourFeature
   ```
3. Make changes and commit them:
   ```bash
   git commit -m 'Add new feature'
   ```
4. Push to your branch:
   ```bash
   git push origin feature/YourFeature
   ```
5. Create a pull request.

## License

This project is licensed under the MIT License; see the [LICENSE](LICENSE) file for details.

---


Citations:
[1] https://www.atommobility.com/products/ride-hailing
[2] https://timesofindia.indiatimes.com/city/pune/autorickshaw-hailing-app-to-bar-ride-refusals-transparent-mobility-services-for-commuters/articleshow/108250395.cms
[3] https://thecityfix.com/blog/smartphone-apps-ease-auto-rickshaw-rides-in-mumbai/
[4] https://orickshaw.com/about/
[5] https://inc42.com/buzz/paytm-eyeing-foray-into-ride-hailing-space-with-autorickshaw-offerings-to-challenge-ola-uber/
[6] https://economictimes.indiatimes.com/tech/technology/paytm-app-launches-auto-rickshaw-booking-feature-via-ondc/articleshow/109981152.cms
[7] https://play.google.com/store/apps/details?hl=en_IN&id=in.juspay.nammayatri
[8] https://www.linkedin.com/pulse/how-build-on-demand-auto-rickshaw-app-like-jugnoo-synarion-it-rclmf
