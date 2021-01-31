import java.util.concurrent.locks.ReentrantLock


/**
 * Bank implementation.
 *
 *
 * :
 *
 * @author : Lukonin Arseny
 */
/**
 * Creates new bank instance.
 *
 * @param n the number of accounts (numbered from 0 to n-1).
 */
class BankImpl (n: Int) : Bank {
    /**
     * An array of accounts by index.
     */
    private val accounts: Array<Account> = Array(n) { Account() }

    override val numberOfAccounts: Int
        get() = accounts.size

    /**
     *
     * :TODO: DONE
     */
    override val totalAmount: Long
        get() {
            try {
                var sum: Long = 0
                for (account in accounts) {
                    account.lock.lock()
                    sum += account.amount
                }
                return sum
            } finally {
                for (account in accounts) {
                    account.lock.unlock()
                }
            }
        }

    /**
     *
     * :TODO: DONE
     */
    override fun getAmount(index: Int): Long {
        accounts[index].lock.lock()
        try {
            return accounts[index].amount
        } finally {
            accounts[index].lock.unlock()
        }
    }

    /**
     *
     * :TODO: DONE
     */
    override fun deposit(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.lock()
        try {
            check(!(amount > Bank.MAX_AMOUNT || account.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
            account.amount += amount
            return account.amount
        } finally {
            account.lock.unlock()
        }
    }

    /**
     *
     * :TODO: DONE
     */
    override fun withdraw(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.lock()
        try {
            check(account.amount - amount >= 0) { "Underflow" }
            account.amount -= amount
            return account.amount
        } finally {
            account.lock.unlock()
        }
    }

    /**
     *
     * :TODO: DONE.
     */
    override fun transfer(fromIndex: Int, toIndex: Int, amount: Long) {
        require(amount > 0) { "Invalid amount: $amount" }
        require(fromIndex != toIndex) { "fromIndex == toIndex" }
        val from = accounts[fromIndex]
        val to = accounts[toIndex]
        if (fromIndex < toIndex) {
            from.lock.lock()
            to.lock.lock()
        } else {
            to.lock.lock()
            from.lock.lock()
        }
        try {
            check(amount <= from.amount) { "Underflow" }
            check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
            from.amount -= amount
            to.amount += amount
        } finally {
            if (fromIndex < toIndex) {
                to.lock.unlock()
                from.lock.unlock()
            } else {
                from.lock.unlock()
                to.lock.unlock()
            }
        }
    }

    /**
     * Private account data structure.
     */
    internal class Account {
        /**
         * Amount of funds in this account.
         */
        var lock = ReentrantLock()
        var amount: Long = 0
    }
}