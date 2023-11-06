<script>
    import {roundNumber} from "../GameState.js";
    import {CommandType, messageStore, playerId} from "../socket.js";

    let bidInput = 0
</script>

<form id="placeBid" on:submit|preventDefault={() => messageStore.send({type: CommandType.PlaceBid, bid: bidInput, actor: $playerId})}>
    <label>Bid <input bind:value={bidInput} type="number" name="bid" min="0" max="{$roundNumber}"></label>
    <input type="submit" value="Place Bid"/>
    <p id="biddingError"></p>
</form>