import {
    DisconnectGameEventListener, ErrorCode,
    getPlayerId,
    listenToNotifications,
    Notification,
    sendCommand,
    socket
} from "../Socket";
import {randomFill} from "crypto";
import {CommandType, NotificationType} from "../Constants";

export class BiddingElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToNotifications({
            [NotificationType.RoundStarted]: this.showForm,
            [NotificationType.BiddingCompleted]: this.hideForm,
        })
    }

    showForm = ({roundNumber}: Notification) => {
        this.replaceChildren()
        this.innerHTML = `
            <form id="placeBid">
                <label>Bid <input type="number" name="bid" min="0" max="${roundNumber}"></label>
                <button type="button" disabled>Place Bid</button>
                <p id="biddingError"></p>
            </form>
        `

        const placeBidForm = this.querySelector("#placeBid") as HTMLFormElement;
        const placeBidBtn = this.querySelector("#placeBid") as HTMLButtonElement;
        const bidInput = this.querySelector(`input[name="bid"]`) as HTMLInputElement;
        const biddingError = this.querySelector("#biddingError") as HTMLParagraphElement;

        bidInput.oninput = (e) => {
            const bid = bidInput.value.replace(/[^0-9]/g, '')
            bidInput.value = bid.toString()
            // @ts-ignore
            if (bid >= 0 && bid <= roundNumber) {
                placeBidBtn.disabled = false
                biddingError.innerText = ""
                biddingError.removeAttribute("errorCode")
                return
            }

            placeBidBtn.disabled = true
            biddingError.innerText = `Bid must be between 0 and ${roundNumber}`
            biddingError.setAttribute("errorCode", ErrorCode.BidLessThan0OrGreaterThanRoundNumber)
        }

        placeBidForm.onsubmit = (e) => {
            e.preventDefault()
            e.stopImmediatePropagation()
            this.hideForm()
            sendCommand({
                type: CommandType.PlaceBid,
                bid: parseInt(bidInput.value),
                actor: getPlayerId(),
            }).catch((reason) => { throw reason })
        }
    }

    hideForm = () => this.replaceChildren()

    disconnectedCallback() {
        if (this.disconnectFn) this.disconnectFn()
        this.disconnectFn = undefined
    }
}

customElements.define("sk-biddingform", BiddingElement)
