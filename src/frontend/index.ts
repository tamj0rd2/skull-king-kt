import {MessageToClient, listenToGameEvents} from "./GameEvents";
export {BiddingElement} from "./components/BiddingElement";
export {BidsElement} from "./components/BidsElement";
export {HandElement} from "./components/HandElement";
export {GameStateElement} from "./components/GameStateElement";
export {GamePhaseElement} from "./components/GamePhaseElement";
export {PlayersElement} from "./components/PlayersElement";
export {TrickElement} from "./components/TrickElement";

declare global {
    var socket: WebSocket
}

const roundNumberEl = document.querySelector("#roundNumber") as HTMLHeadingElement
const trickNumberEl = document.querySelector("#trickNumber") as HTMLHeadingElement
const playerWhoseTurnItIsEl = document.querySelector("#currentPlayer") as HTMLHeadingElement
listenToGameEvents({
    [MessageToClient.RoundStarted]: ({roundNumber}) => {
        roundNumberEl.innerText = `Round ${roundNumber}`
        roundNumberEl.setAttribute("data-roundNumber", roundNumber)

        trickNumberEl.innerText = ""
        trickNumberEl.removeAttribute("data-trickNumber")
    },
    [MessageToClient.TrickStarted]: ({trickNumber, firstPlayer}) => {
        trickNumberEl.innerText = `Trick ${trickNumber}`
        trickNumberEl.setAttribute("data-trickNumber", trickNumber)

        playerWhoseTurnItIsEl.innerText = firstPlayer ? "" : firstPlayer
        playerWhoseTurnItIsEl.setAttribute("data-playerId", firstPlayer)
    },
    [MessageToClient.CardPlayed]: ({nextPlayer}) => {
        playerWhoseTurnItIsEl.innerText = nextPlayer ? "" : nextPlayer
        playerWhoseTurnItIsEl.setAttribute("data-playerId", nextPlayer)
    }
})

socket.addEventListener("close", (event) => {
    console.error("disconnected from ws")
})
