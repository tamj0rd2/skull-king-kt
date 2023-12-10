<script>
    import {roundNumber} from "../GameState";
    import {messageStore, playerId} from "../socket";
    import {PlayerCommand} from "../../generated_types.ts";

    let bidInput = 0
</script>

<form id="placeBid" on:submit|preventDefault={() => messageStore.send({type: PlayerCommand.Type.PlaceBid, bid: bidInput, actor: $playerId})}>
    <label>Bid <input bind:value={bidInput} type="number" name="bid" min="0" max={$roundNumber}></label>
    <input type="submit" value="Place Bid"/>
    <p id="biddingError"></p>
</form>