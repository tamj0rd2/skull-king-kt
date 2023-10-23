import {listenToNotifications, sendCommand, setPlayerId} from "./Socket";
import {CommandType, NotificationType, PlayerId} from "./Constants";

export {BiddingElement} from "./components/BiddingElement";
export {BidsElement} from "./components/BidsElement";
export {HandElement} from "./components/HandElement";
export {GameStateElement} from "./components/GameStateElement";
export {GamePhaseElement} from "./components/GamePhaseElement";
export {PlayersElement} from "./components/PlayersElement";
export {TrickElement} from "./components/TrickElement";
export {WinsElement} from "./components/WinsElement";

const joinGameEl = document.forms.namedItem("joinGame")!!
joinGameEl.addEventListener("submit",  (e) => {
    e.preventDefault()
    e.stopImmediatePropagation()
    const formData = new FormData(joinGameEl)
    const playerId = formData.get("playerId") as PlayerId
    setPlayerId(playerId)
    sendCommand({type: CommandType.JoinGame, actor: playerId})
        .catch((reason) => { throw reason })
})

const roundNumberEl = document.querySelector("#roundNumber") as HTMLHeadingElement
const trickNumberEl = document.querySelector("#trickNumber") as HTMLHeadingElement
const playerWhoseTurnItIsEl = document.querySelector("#currentPlayer") as HTMLHeadingElement
const trickWinnerEl = document.querySelector("#trickWinner") as HTMLHeadingElement
listenToNotifications({
    [NotificationType.YouJoined]: ({playerId}) => {
        setPlayerId(playerId)
        document.querySelector("h1")!!.textContent = `Game Page - ${playerId}`
    },
    [NotificationType.RoundStarted]: ({roundNumber}) => {
        roundNumberEl.innerText = `Round ${roundNumber}`
        roundNumberEl.setAttribute("data-roundNumber", roundNumber)

        trickNumberEl.innerText = ""
        trickNumberEl.removeAttribute("data-trickNumber")

        trickWinnerEl.innerText = ""
        trickWinnerEl.removeAttribute("data-playerId")
    },
    [NotificationType.TrickStarted]: ({trickNumber, firstPlayer}) => {
        trickNumberEl.innerText = `Trick ${trickNumber}`
        trickNumberEl.setAttribute("data-trickNumber", trickNumber)

        playerWhoseTurnItIsEl.innerText = `Current player: ${firstPlayer}`
        playerWhoseTurnItIsEl.setAttribute("data-playerId", firstPlayer)

        trickWinnerEl.innerText = ""
        trickWinnerEl.removeAttribute("data-playerId")
    },
    [NotificationType.CardPlayed]: ({nextPlayer}) => {
        playerWhoseTurnItIsEl.innerText = nextPlayer ? `Current player: ${nextPlayer}` : ""
        playerWhoseTurnItIsEl.setAttribute("data-playerId", nextPlayer)
    },
    [NotificationType.TrickCompleted]: ({winner}) => {
        trickWinnerEl.innerText = `Winner: ${winner}`
        trickWinnerEl.setAttribute("data-playerId", winner)
    }
})
