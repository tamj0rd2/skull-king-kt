function connectToWs(wsAddress) {
    const socket = new WebSocket(wsAddress);

    configureWs()

    function configureWs() {
        const body = document.querySelector("body")
        const biddingForm = document.createElement("sk-biddingform")
        const bidsEl = document.createElement("sk-bids")

        function updateGamePhase(gamePhase) {
            const gamePhaseEl = document.getElementById("gamePhase")
            const gamePhaseMapping = {
                "Bidding": "Place your bid!",
                "TrickTaking": "It's trick taking time!",
                "TrickComplete": "Trick completed :)"
            }

            const text = gamePhaseMapping[gamePhase]
            if (!text) throw new Error("Unknown game phase: " + gamePhase)
            gamePhaseEl.innerText = gamePhaseMapping[gamePhase]
        }

        socket.addEventListener("close", (event) => {
            console.error("disconnected from ws")
        })

        socket.addEventListener("message", (event) => {
            console.log("Message from server ", event.data);

            try {
                const data = JSON.parse(event.data)
                switch (data.type) {
                    case "GameEvent$PlayerJoined":
                        const players = document.getElementById("players")
                        const li = document.createElement("li")
                        li.innerText = data.playerId
                        players.appendChild(li)

                        // TODO: I don't think I need this anymore? Or I should have a message saying waiting to start
                        if (!data.waitingForMorePlayers) {
                            const gameStateEl = document.getElementById("gameState")
                            gameStateEl.innerText = ""
                        }
                        return
                    case "GameEvent$GameStarted":
                        const gameStateEl = document.getElementById("gameState")
                        gameStateEl.innerText = "The game has started :D"
                        body.appendChild(biddingForm)

                        body.appendChild(bidsEl)
                        const gameStateEl1 = document.getElementById("gameState")
                        gameStateEl1.innerText = "The game has started :D"
                        body.appendChild(biddingForm)

                        body.appendChild(bidsEl)
                        return bidsEl.gameStarted(data.players);
                    case "GameEvent$RoundStarted":
                        const roundNumberEl = document.getElementById("roundNumber")
                        roundNumberEl.innerText = data.roundNumber
                        updateGamePhase("Bidding")

                        const trickNumberEl = document.getElementById("trickNumber")
                        trickNumberEl.innerText = ""

                        biddingForm.handleRoundStarted()

                        const trick = document.getElementById("trick")
                        trick.textContent = ""

                        const handEl = document.getElementById("hand")
                        return data.cardsDealt.forEach(card => {
                            const li = document.createElement("li")
                            li.innerText = card.id

                            const button = document.createElement("button")
                            button.innerText = "Play"
                            button.onclick = function playCard() {
                                li.remove()
                                socket.send(JSON.stringify({
                                    type: "ClientMessage$CardPlayed",
                                    cardId: card.id,
                                }))
                            }
                            li.appendChild(button)
                            handEl.appendChild(li)
                        });
                    case "GameEvent$BidPlaced":
                        let {playerId} = data;
                        return bidsEl.handleBidPlaced(playerId);
                    case "GameEvent$BiddingCompleted":
                        let {bids} = data;
                        updateGamePhase("TrickTaking")
                        return bidsEl.handleBiddingCompleted(bids);
                    case "GameEvent$CardPlayed":
                        const trick1 = document.getElementById("trick")
                        const li1 = document.createElement("li")
                        li1.innerText = `${data.playerId}:${data.cardId}`
                        return trick1.appendChild(li1);
                    case "GameEvent$TrickCompleted":
                        return updateGamePhase("TrickComplete");
                    case "GameEvent$TrickStarted":
                        const trickNumberEl1 = document.getElementById("trickNumber")
                        trickNumberEl1.innerText = data.trickNumber

                        const trick2 = document.getElementById("trick")
                        return trick2.textContent = "";
                    case "GameEvent$GameCompleted":
                        const gameStateEl2 = document.getElementById("gameState")
                        return gameStateEl2.innerText = "The game is over!";
                    default: {
                        socket.send(JSON.stringify({
                            type: "ClientMessage$UnhandledMessageFromServer",
                            offender: data.type,
                        }))
                        console.error(`Unknown message from server: ${data.type}`)
                    }
                }
            } catch (e) {
                socket.send(JSON.stringify({
                    stackTrace: e.stack,
                    type: "ClientMessage$Error",
                }))
                throw e
            }
        });
    }

    class BiddingForm extends HTMLElement {
        constructor() {
            super()
        }

        connectedCallback() {
            this.innerHTML = `
                <label>Bid <input type="number" name="bid" min="0" max="10"></label>
                <button id="placeBid" type="button" onclick="onBidSubmit()">Place Bid</button>
                <p id="biddingError"></p>
            `

            const bidInput = this.querySelector(`input[name="bid"]`)
            this.querySelector("#placeBid").onclick = function () {
                socket.send(JSON.stringify({
                    type: "ClientMessage$BidPlaced",
                    bid: bidInput.value,
                }))
            }
        }

        handleRoundStarted() {
            this.querySelector(`input[name="bid"]`).value = ""
        }
    }

    class Bids extends HTMLElement {
        constructor() {
            super();
        }

        connectedCallback() {
            this.innerHTML = `
                <section id="bidsArea">
                    <h3>Bids</h3>
                    <ul id="bids"></ul>
                </section>
            `
        }

        gameStarted(playerIds) {
            const bids = this.querySelector("#bids")
            playerIds.forEach(playerId => {
                const li = document.createElement("li")
                li.textContent = playerId
                li.setAttribute("data-playerBid", playerId)
                li.appendChild(document.createElement("span"))
                bids.appendChild(li)
            })
        }

        handleBidPlaced(playerId) {
            this.querySelector(`[data-playerBid="${playerId}"] span`).innerText = ":" + "has bid"
        }

        handleBiddingCompleted(bids) {
            this.querySelectorAll(`[data-playerBid]`).forEach(el => {
                const playerId = el.getAttribute("data-playerBid")
                const bid = bids[playerId]
                el.querySelector("span").innerText = ":" + bid
            })
        }
    }

    customElements.define("sk-biddingform", BiddingForm)
    customElements.define("sk-bids", Bids)
}
