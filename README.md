# Connect4-Online

Connect4-Online is a Java-based multiplayer implementation of the classic Connect 4 game, designed to allow multiple clients to play games simultaneously via a central server. The server uses threading to manage concurrent connections, enabling seamless gameplay experiences for all participants.

## Features

- **Multiplayer Support:** Multiple clients can connect to the server and play games at the same time.
- **Threaded Server:** The server employs Java threading to handle multiple games and client connections concurrently.
- **Classic Gameplay:** Enjoy the timeless Connect 4 game with friends or other online players.

## How It Works

- The server listens for incoming connections from clients.
- Each client connection is handled in a separate thread, allowing for multiple games to occur in parallel.
- Clients communicate with the server to start new games, make moves, and receive updates on game state.

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 8 or higher

### Setup

1. **Clone the repository:**

   ```bash
   git clone https://github.com/npebler/Connect4-Online.git
   cd Connect4-Online
   ```

2. **Compile the project:**

   ```bash
   javac *.java
   ```

3. **Start the server:**

   ```bash
   java Connect4Server
   ```

4. **Start a client (repeat for multiple players):**

   ```bash
   java Connect4Client
   ```

## Usage

- When the server is running, clients can connect by running the client application.
- Follow the on-screen instructions to join or start a game.
- The server will pair clients into games and manage game state and communication.

## Project Structure

- `Connect4Server.java` - Main server application; handles client connections and game management.
- `Connect4Client.java` - Client application; connects to the server to play the game.

## Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.

## License

This project is open source and available under the [MIT License](LICENSE).

## Acknowledgements

- Classic Connect 4 game concept
- Java threading documentation
