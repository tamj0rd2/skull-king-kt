import {
    DisconnectGameEventListener, ErrorCode,
    getPlayerId,
    listenToNotifications,
    sendCommand,
    socket
} from "../Socket";
import {Notification, PlayerCommand} from "../generated_types";

export class BiddingElement extends HTMLElement {
    disconnectFn?: DisconnectGameEventListener

    constructor() {
        super()
    }

    connectedCallback() {
        this.disconnectedCallback()
        this.disconnectFn = listenToNotifications({
            [Notification.Type.RoundStarted]: this.showForm,
            [Notification.Type.BiddingCompleted]: this.hideForm,
        })
    }

    showForm = ({roundNumber}: Notification.RoundStarted) => {
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
                type: PlayerCommand.Type.PlaceBid,
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
