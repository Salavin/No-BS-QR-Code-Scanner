package `in`.samlav.processqrcode

const val APPEND_ERROR = "append_error"

class QRCodeOptions
{
    var appendError = true

    public fun setOption(key: String, value: Boolean): Boolean?
    {
        when (key)
        {
            APPEND_ERROR -> appendError = value
            else -> return null
        }
        return true
    }

    public fun getOption(key: String): Boolean?
    {
        return when (key)
        {
            APPEND_ERROR -> appendError
            else -> null
        }
    }
}