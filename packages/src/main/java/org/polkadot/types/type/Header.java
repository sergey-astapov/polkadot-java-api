package org.polkadot.types.type;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.polkadot.types.Types;
import org.polkadot.types.TypesUtils;
import org.polkadot.types.codec.Compact;
import org.polkadot.types.codec.Struct;
import org.polkadot.types.primitive.U64;
import org.polkadot.utils.CryptoUtils;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * @name Header
 * A [[Block]] header
 */
public class Header extends Struct {

    //  constructor (value?: HeaderValue | Uint8Array | null) {
    public Header(Object value) {
        super(new Types.ConstructorDef()
                        .add("parentHash", Hash.class)
                        .add("number", Compact.with(TypesUtils.getConstructorCodec(BlockNumber.class)))
                        .add("stateRoot", Hash.class)
                        .add("extrinsicsRoot", Hash.class)
                        .add("digest", Digest.class)
                , value == null ? new LinkedHashMap<>() : value);

    }


    /**
     * The wrapped [[BlockNumber]]
     */
    public BlockNumber getBlockNumber() {
        return (BlockNumber) ((Compact) this.getField("number")).toBn();
    }


    /**
     * The wrapped [[Digest]]
     */
    public Digest getDigest() {
        return this.getField("digest");
    }

    /**
     * The wrapped extrisics root as a [[Hash]]
     */
    public Hash getExtrinsicsRoot() {
        return this.getField("extrinsicsRoot");
    }

    /**
     * Convenience method, encodes the header and calculates the [[Hash]]
     */
    public Hash getHash() {
        byte[] bytes = CryptoUtils.blake2AsU8a(this.toU8a(), 256);
        return new Hash(bytes);
    }

    /**
     * Alias for `blockNumber` (this is displayed in JSON)
     */
    public BlockNumber getNumber() {
        return this.getBlockNumber();
    }

    /**
     * The wrapped parent as a [[Hash]]
     */
    public Hash getParentHash() {
        return this.getField("parentHash");
    }

    /**
     * The wrapped state root as a [[Hash]]
     */

    public Hash getStateRoot() {
        return this.getField("stateRoot");
    }


    public static class HeaderValue {
        //digest?: Digest | { logs: DigestItem[] },
        //extrinsicsRoot?: AnyU8a,
        //number?: AnyNumber,
        //parentHash?: AnyU8a,
        //stateRoot?: AnyU8a
        Digest digest;
        byte[] extrinsicsRoot;
        int number;
        byte[] parentHash;
        byte[] stateRoot;
    }


    /**
     * @name HeaderExtended
     * @description A [[Block]] header with an additional `author` field that indicates the block author
     */
    public static class HeaderExtended extends Header {
        private AccountId author;

        //  constructor (header: Header | null = null, sessionValidators: Array<AccountId> = []) {
        public HeaderExtended(Header header, List<AccountId> sessionValidators) {
            super(header);

            if (header == null || header.getDigest() == null || CollectionUtils.isEmpty(sessionValidators)) {
                return;
            }

            Digest.DigestItem item = header.getDigest().getLogs().stream().filter(log -> log.isConsensus()).findFirst().orElse(null);
            U64 slot = null;

            // extract author from the consensus (substrate 1.0, digest)
            if (item != null) {
                Digest.Consensus consensus = item.getAsConsensus();

                if (consensus.isAura()) {
                    slot = consensus.asAura().getKey();
                }
            } else {
                item = header.getDigest().getLogs().stream().filter(log -> log.isSeal()).findFirst().orElse(null);

                // extract author from the seal (pre substrate 1.0, backwards compat)
                if (item != null) {
                    slot = item.getAsSeal().slot();
                }
            }

            // found a slot? Great, extract the validator
            if (slot != null) {
                this.author = sessionValidators.get(slot.intValue() % sessionValidators.size());
            }
        }


        /**
         * @description Convenience method, returns the author for the block
         */
        public AccountId getAuthor() {
            return this.author;
        }


        /**
         * @description Creates the JSON representation
         */

        @Override
        public Object toJson() {

            JSONObject jsonObject = (JSONObject) super.toJson();
            jsonObject.put("author", this.author != null ? this.author.toJson() : null);
            return jsonObject;
        }
    }
}