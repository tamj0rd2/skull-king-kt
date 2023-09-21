import {listenToGameEvents, MessageToClient} from "./GameEvents";

enum GameMasterCommandType {
    StartGame = "GameMasterCommand$StartGame",
    StartNextRound = "GameMasterCommand$StartNextRound",
    StartNextTrick = "GameMasterCommand$StartNextTrick",
}

declare global {
    var INITIAL_ADMIN_STATE: {
        wsHost: string
    }
}

async function doGameMasterCommand(type: GameMasterCommandType) {
    const res = await fetch("/do-game-master-command", {
        method: "POST",
        body: JSON.stringify({type})
    })
    if (!res.ok) return alert("Error starting game")
}

function after1Second(commandType: GameMasterCommandType) {
    console.log(`Sending ${commandType} in 1 second...`)
    setTimeout(() => doGameMasterCommand(commandType), 1000)
}

const startGameButton = document.querySelector("#startGame") as HTMLButtonElement
startGameButton.onclick = () => doGameMasterCommand(GameMasterCommandType.StartGame)

const startNextRoundButton = document.querySelector("#startRound") as HTMLButtonElement
startNextRoundButton.onclick = () => doGameMasterCommand(GameMasterCommandType.StartNextRound)

const startNextTrickButton = document.querySelector("#startTrick") as HTMLButtonElement
startNextTrickButton.onclick = () => doGameMasterCommand(GameMasterCommandType.StartNextTrick)

const automateCommandsButton = document.querySelector("#automateCommands") as HTMLButtonElement
automateCommandsButton.onclick = () => {
    console.log("automating commands, or something...")

    const socket = new WebSocket(window.INITIAL_ADMIN_STATE.wsHost + "/admin")
    socket.onopen = () => console.log("socket opened")
    socket.onclose = () => console.log("socket closed")
    socket.onerror = (event) => console.log("error", event)
    window.socket = socket

    var savedRoundNumber = 0
    var savedTrickNumber = 0
    listenToGameEvents({
        [MessageToClient.RoundStarted]: ({ roundNumber }) =>savedRoundNumber = roundNumber,
        [MessageToClient.TrickStarted]: ({ trickNumber })=> savedTrickNumber = trickNumber,
        [MessageToClient.PlayerJoined]: ({ waitingForMorePlayers }) => {
            if (!waitingForMorePlayers) after1Second(GameMasterCommandType.StartGame)
        },
        [MessageToClient.BiddingCompleted]: () => after1Second(GameMasterCommandType.StartNextTrick),
        [MessageToClient.TrickCompleted]: () => {
            const commandType = savedTrickNumber === savedRoundNumber
                ? GameMasterCommandType.StartNextRound
                : GameMasterCommandType.StartNextTrick
            after1Second(commandType)
        }
    })
}
