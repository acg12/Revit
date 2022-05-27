class ComparatorSaved {
    companion object: Comparator<HashMap<String, Any>> {
        override fun compare(
            p0: java.util.HashMap<String, Any>?,
            p1: java.util.HashMap<String, Any>?,
        ): Int {
            val count1 = p0!!.get("count") as Int
            val count2 = p1!!.get("count") as Int

            return count1 - count2
        }
    }
}