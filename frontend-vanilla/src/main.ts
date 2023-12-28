import "./style.css"
import {listenToNotifications, NackError, sendCommand, setPlayerId} from "./Socket";
import {showErrorMessage} from "./ErrorHandling";
import {Notification, PlayerCommand, PlayerId} from "../generated_types";

export {BiddingElement} from "./components/BiddingElement";
export {BidsElement} from "./components/BidsElement";
export {HandElement} from "./components/HandElement";
export {GameStateElement} from "./components/GameStateElement";
export {GamePhaseElement} from "./components/GamePhaseElement";
export {PlayersElement} from "./components/PlayersElement";
export {TrickElement} from "./components/TrickElement";
export {WinsElement} from "./components/WinsElement";

const joinGameEl = document.forms.namedItem("joinGame")!!
joinGameEl.addEventListener("submit", (e) => {
    e.preventDefault()
    e.stopImmediatePropagation()
    const formData = new FormData(joinGameEl)
    const playerId = formData.get("playerId") as PlayerId
    setPlayerId(playerId)
    sendCommand({type: PlayerCommand.Type.JoinGame, actor: playerId})
        .catch((e) => {
            console.error(`${playerId} failed to join game`)
            if (e instanceof NackError) showErrorMessage("Failed to join the game", e.reason)
            else showErrorMessage(`Failed to join the game - ${e}`)
        })
})

const roundNumberEl = document.querySelector("#roundNumber") as HTMLHeadingElement
const trickNumberEl = document.querySelector("#trickNumber") as HTMLHeadingElement
const playerWhoseTurnItIsEl = document.querySelector("#currentPlayer") as HTMLHeadingElement
const trickWinnerEl = document.querySelector("#trickWinner") as HTMLHeadingElement
listenToNotifications({
    [Notification.Type.YouJoined]: ({playerId}) => {
        setPlayerId(playerId)
        document.querySelector("h1")!!.textContent = `Game Page - ${playerId}`
    },
    [Notification.Type.RoundStarted]: ({roundNumber}) => {
        roundNumberEl.innerText = `Round ${roundNumber}`
        roundNumberEl.setAttribute("data-roundNumber", roundNumber.toString())

        trickNumberEl.innerText = ""
        trickNumberEl.removeAttribute("data-trickNumber")

        trickWinnerEl.innerText = ""
        trickWinnerEl.removeAttribute("data-playerId")
    },
    [Notification.Type.TrickStarted]: ({trickNumber, firstPlayer}) => {
        trickNumberEl.innerText = `Trick ${trickNumber}`
        trickNumberEl.setAttribute("data-trickNumber", trickNumber.toString())

        playerWhoseTurnItIsEl.innerText = `Current player: ${firstPlayer}`
        playerWhoseTurnItIsEl.setAttribute("data-playerId", firstPlayer)

        trickWinnerEl.innerText = ""
        trickWinnerEl.removeAttribute("data-playerId")
    },
    [Notification.Type.CardPlayed]: ({nextPlayer}) => {
        playerWhoseTurnItIsEl.innerText = nextPlayer ? `Current player: ${nextPlayer}` : ""
        nextPlayer && playerWhoseTurnItIsEl.setAttribute("data-playerId", nextPlayer)
    },
    [Notification.Type.TrickCompleted]: ({winner}) => {
        trickWinnerEl.innerText = `Winner: ${winner}`
        trickWinnerEl.setAttribute("data-playerId", winner)
    }
})
