import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicReference

/**
 * Int-to-Int hash map with open addressing and linear probes.
 *
 * TODO: This class is **NOT** thread-safe.
 */
class IntIntHashMap {
    private val core = AtomicReference(Core(INITIAL_CAPACITY))

    /**
     * Returns value for the corresponding key or zero if this key is not present.
     *
     * @param key a positive key.
     * @return value for the corresponding or zero if this key is not present.
     * @throws IllegalArgumentException if key is not positive.
     */
    operator fun get(key: Int): Int {
        require(key > 0) { "Key must be positive: $key" }
        var oldValue: Int
        while (true) {
            val _core = core.get()
            oldValue = _core.getInternal(key)
            if (oldValue != NEEDS_REHASH) break
            //need to rehash otherwise
            if (_core === core.get()){
                _core.rehash()
                core.compareAndSet(_core, _core.next.get())
            }
        }
        return toValue(oldValue)
    }

    /**
     * Changes value for the corresponding key and returns old value or zero if key was not present.
     *
     * @param key   a positive key.
     * @param value a positive value.
     * @return old value or zero if this key was not present.
     * @throws IllegalArgumentException if key or value are not positive, or value is equal to
     * [Integer.MAX_VALUE] which is reserved.
     */
    fun put(key: Int, value: Int): Int {
        require(key > 0) { "Key must be positive: $key" }
        require(isValue(value)) { "Invalid value: $value" }
        return toValue(putAndRehashWhileNeeded(key, value))
    }

    /**
     * Removes value for the corresponding key and returns old value or zero if key was not present.
     *
     * @param key a positive key.
     * @return old value or zero if this key was not present.
     * @throws IllegalArgumentException if key is not positive.
     */
    fun remove(key: Int): Int {
        require(key > 0) { "Key must be positive: $key" }
        return toValue(putAndRehashWhileNeeded(key, DEL_VALUE))
    }

    private fun putAndRehashWhileNeeded(key: Int, value: Int): Int {
        while (true) {
            val _core = core.get()
            val oldValue = _core.putInternal(key, value)
            if (oldValue != NEEDS_REHASH) return oldValue
            //need to rehash otherwise
            if (_core === core.get()){
                _core.rehash()
                core.compareAndSet(_core, _core.next.get())
            }
        }
    }

    private class Core internal constructor(capacity: Int) {
        // Pairs of <key, value> here, the actual
        // size of the map is twice as big.
        val map: AtomicIntegerArray
        val shift: Int
        val next: AtomicReference<Core>

        init {
            map = AtomicIntegerArray(2 * capacity)
            val mask = capacity - 1
            assert(mask > 0 && mask and capacity == 0) { "Capacity must be power of 2: $capacity" }
            shift = 32 - Integer.bitCount(mask)
            next = AtomicReference()
        }

        fun getInternal(key: Int): Int {
            var index = index(key)
            var probes = 0
            while (true) { // optimize for successful lookup
                val _val = map.get(index + 1)
                val _key = map.get(index)
                if (_val == MOVED) return NEEDS_REHASH
                if (_key == NULL_KEY) return NULL_VALUE // not found -- no value
                if (_key == key) return unmark(_val)
                if (++probes >= MAX_PROBES) return NULL_VALUE
                if (index == 0) index = map.length()
                index -= 2
            }
        }

        fun putInternal(key: Int, value: Int): Int {
            var index = index(key)
            var probes = 0
            while (true) { // optimize for successful lookup
                val _val = map.get(index + 1)
                val _key = map.get(index)
                if (isMarked(_val)) return NEEDS_REHASH
                if (_key == NULL_KEY) {
                    // not found -- claim this slot
                    if (value == DEL_VALUE) {
                        return NULL_VALUE
                    } // remove of missing item, no need to claim slot
                    if (map.compareAndSet(index, _key, key)) {
                        if (map.compareAndSet(index + 1, _val, value)) {
                            return _val
                        }
                    }
                    continue
                }
                if (key == _key) {
                    if (map.compareAndSet(index + 1, _val, value)) {
                        return _val
                    } else {
                        continue
                    }
                }
                if (++probes >= MAX_PROBES) return NEEDS_REHASH
                if (index == 0) index = map.length()
                index -= 2
            }
        }

        fun rehash() {
            next.compareAndSet(null, Core(map.length())) // map.length is twice the current capacity
            var index = 0
            while (index < map.length()) {
                val _val = map.get(index + 1)
                val _key = map.get(index)
                if (_val == MOVED) {
                    index += 2
                    continue
                }
                if (!isMarked(_val)) {
                    if (_val == DEL_VALUE) {
                        if (!map.compareAndSet(index + 1, _val, MOVED)){
                            continue
                        }
                    }
                    if (!map.compareAndSet(index + 1, _val, mark(_val))){
                        continue
                    }
                }
                val unmarked = unmark(_val)
                if (isValue(unmarked)) {
                    next.get().rehashOther(_key, unmarked)
                    map.compareAndSet(index + 1, _val, MOVED)
                }
                index += 2
            }
        }

        fun rehashOther(key: Int, value: Int) {
            var index = index(key)
            var probes = 0
            while (true) {
                val _key = map.get(index)
                if (_key == NULL_KEY) {
                    if (map.compareAndSet(index, _key, key) && map.compareAndSet(index + 1, NULL_VALUE, value)) {
                        return
                    } else {
                        continue
                    }
                }
                if (_key == key){
                    map.compareAndSet(index + 1, NULL_VALUE, value)
                    return
                }
                if (++probes >= MAX_PROBES) return
                if (index == 0) index = map.length()
                index -= 2
            }
        }

        /**
         * Returns an initial index in map to look for a given key.
         */
        fun index(key: Int): Int = (key * MAGIC ushr shift) * 2
    }
}

private const val MAGIC = -0x61c88647 // golden ratio
private const val INITIAL_CAPACITY = 2 // !!! DO NOT CHANGE INITIAL CAPACITY !!!
private const val MAX_PROBES = 8 // max number of probes to find an item
private const val NULL_KEY = 0 // missing key (initial value)
private const val NULL_VALUE = 0 // missing value (initial value)
private const val DEL_VALUE = Int.MAX_VALUE // mark for removed value
private const val MOVED = Int.MIN_VALUE //S nodes (already have been moved)
private const val NEEDS_REHASH = -1 // returned by `putInternal` to indicate that rehash is needed

// Checks is the value is in the range of allowed values
private fun isValue(value: Int): Boolean = value in (1 until DEL_VALUE)

// Converts internal value to the public results of the methods
private fun toValue(value: Int): Int = if (isValue(value)) value else 0

private fun mark(value: Int) : Int {
    return value or (1 shl 31)
}

private fun unmark(value: Int) : Int {
    return value and (1 shl 31).inv()
}

private fun isMarked(value: Int) : Boolean {
    return (value and (1 shl 31)) != 0
}