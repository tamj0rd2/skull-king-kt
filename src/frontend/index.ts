import {EventType, listenToGameEvents} from "./GameEvents";
import {BiddingElement} from "./components/BiddingElement";
import {BidsElement} from "./components/BidsElement";
import {HandElement} from "./components/HandElement";
import {GameStateElement} from "./components/GameStateElement";
import {GamePhaseElement} from "./components/GamePhaseElement";
import {PlayersElement} from "./components/PlayersElement";
import {TrickElement} from "./components/TrickElement";

declare global {
    var socket: WebSocket
    var connectToWs: () => void
}

customElements.define("sk-biddingform", BiddingElement)
customElements.define("sk-bids", BidsElement)
customElements.define("sk-hand", HandElement)
customElements.define("sk-gamestate", GameStateElement)
customElements.define("sk-gamephase", GamePhaseElement)
customElements.define("sk-players", PlayersElement)
customElements.define("sk-trick", TrickElement)

window.connectToWs = () => {
    const body = document.querySelector("body")!!
    const dynamicComponents = [
        document.createElement("sk-gamestate"),
        document.createElement("sk-gamephase"),
        document.createElement("sk-players"),
        document.createElement("sk-hand"),
        document.createElement("sk-biddingform"),
        document.createElement("sk-bids"),
        document.createElement("sk-trick"),
    ]
    dynamicComponents.forEach(component => body.appendChild(component))

    const roundNumberEl = document.querySelector("#roundNumber") as HTMLHeadingElement
    const trickNumberEl = document.querySelector("#trickNumber") as HTMLHeadingElement
    const playerWhoseTurnItIsEl = document.querySelector("#currentPlayer") as HTMLHeadingElement
    listenToGameEvents({
        [EventType.RoundStarted]: ({roundNumber}) => {
            roundNumberEl.innerText = `Round ${roundNumber}`
            roundNumberEl.setAttribute("data-roundNumber", roundNumber)

            trickNumberEl.innerText = ""
            trickNumberEl.removeAttribute("data-trickNumber")
        },
        [EventType.TrickStarted]: ({trickNumber, firstPlayer}) => {
            trickNumberEl.innerText = `Trick ${trickNumber}`
            trickNumberEl.setAttribute("data-trickNumber", trickNumber)

            playerWhoseTurnItIsEl.innerText = firstPlayer ? "" : firstPlayer
            playerWhoseTurnItIsEl.setAttribute("data-playerId", firstPlayer)
        },
        [EventType.CardPlayed]: ({nextPlayer}) => {
            playerWhoseTurnItIsEl.innerText = nextPlayer ? "" : nextPlayer
            playerWhoseTurnItIsEl.setAttribute("data-playerId", nextPlayer)
        }
    })

    socket.addEventListener("close", (event) => {
        console.error("disconnected from ws")
    })
}