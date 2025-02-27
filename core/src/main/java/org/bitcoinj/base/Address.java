/*
 * Copyright by the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.base;

import org.bitcoinj.base.exceptions.AddressFormatException;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.core.NetworkParameters;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * Interface for addresses, e.g. native segwit addresses ({@link SegwitAddress}) or legacy addresses ({@link LegacyAddress}).
 * <p>
 * Use {@link AddressParser} to construct any kind of address from its textual form.
 */
public interface Address extends Comparable<Address> {
    /**
     * Construct an address from its textual form.
     * 
     * @param params the expected network this address is valid for, or null if the network should be derived from the
     *               textual form
     * @param str the textual form of the address, such as "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL" or
     *            "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
     * @return constructed address
     * @throws AddressFormatException
     *             if the given string doesn't parse or the checksum is invalid
     * @throws AddressFormatException.WrongNetwork
     *             if the given string is valid but not for the expected network (eg testnet vs mainnet)
     * @deprecated Use {@link org.bitcoinj.wallet.Wallet#parseAddress(String)} or {@link AddressParser#parseAddress(String)}
     */
    @Deprecated
    static Address fromString(@Nullable NetworkParameters params, String str)
            throws AddressFormatException {
        return AddressParser.getLegacy(params).parseAddress(str);
    }

    /**
     * Construct an {@link Address} that represents the public part of the given {@link ECKey}.
     * 
     * @param params
     *            network this address is valid for
     * @param key
     *            only the public part is used
     * @param outputScriptType
     *            script type the address should use
     * @return constructed address
     * @deprecated Use {@link ECKey#toAddress(ScriptType, Network)}
     */
    @Deprecated
    static Address fromKey(final NetworkParameters params, final ECKey key, final ScriptType outputScriptType) {
        return key.toAddress(outputScriptType, params.network());
    }

    /**
     * @return network this data is valid for
     * @deprecated Use {@link #network()}
     */
    @Deprecated
    default NetworkParameters getParameters() {
        return NetworkParameters.of(network());
    }

    /**
     * Get either the public key hash or script hash that is encoded in the address.
     * 
     * @return hash that is encoded in the address
     */
    byte[] getHash();

    /**
     * Get the type of output script that will be used for sending to the address.
     * 
     * @return type of output script
     */
    ScriptType getOutputScriptType();

    /**
     * Comparison field order for addresses is:
     * <ol>
     *     <li>{@link Network#id()}</li>
     *     <li>Legacy vs. Segwit</li>
     *     <li>(Legacy only) Version byte</li>
     *     <li>remaining {@code bytes}</li>
     * </ol>
     * <p>
     * Implementations use {@link Address#PARTIAL_ADDRESS_COMPARATOR} for tests 1 and 2.
     *
     * @param o other {@code Address} object
     * @return comparison result
     */
    @Override
    int compareTo(Address o);

    /**
     * Get the network this address works on. Use of {@link BitcoinNetwork} is preferred to use of {@link NetworkParameters}
     * when you need to know what network an address is for.
     * @return the Network.
     */
    Network network();

    /**
     * Comparator for the first two comparison fields in {@code Address} comparisons, see {@link Address#compareTo(Address)}.
     * Used by {@link LegacyAddress#compareTo(Address)} and {@link SegwitAddress#compareTo(Address)}.
     * For use by implementing classes only.
     */
    Comparator<Address> PARTIAL_ADDRESS_COMPARATOR = Comparator
        .comparing((Address a) -> a.network().id()) // First compare network
        .thenComparing(Address::compareTypes);      // Then compare address type (subclass)

    /* private */
    static int compareTypes(Address a, Address b) {
        if (a instanceof LegacyAddress && b instanceof SegwitAddress) {
            return -1;  // Legacy addresses (starting with 1 or 3) come before Segwit addresses.
        } else if (a instanceof SegwitAddress && b instanceof LegacyAddress) {
            return 1;
        } else {
            return 0;   // Both are the same type: additional `thenComparing()` lambda(s) for that type must finish the comparison
        }
    }
}
