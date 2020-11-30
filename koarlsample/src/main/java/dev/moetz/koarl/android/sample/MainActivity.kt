package dev.moetz.koarl.android.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.moetz.koarl.android.Koarl
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnNonFatalException.setOnClickListener {
            Koarl.logException(
                RuntimeException(
                    "Manually triggered non-fatal exception",
                    RuntimeException(
                        "First Cause for a manually triggered exception",
                        RuntimeException(
                            "Second Cause for a manually triggered exception",
                            RuntimeException("Third Cause for a manually triggered exception")
                        )
                    )
                )
            )
        }

        btnFatalExceptionMain.setOnClickListener {
            throw RuntimeException(
                "Manually triggered fatal exception on main thread"
            )
        }

        btnBulkNonFatalExceptions.setOnClickListener {
            val threads = (0..5).map { outerCount ->
                thread(start = false) {
                    repeat(3) { innerCount ->
                        Koarl.logException(
                            RuntimeException(
                                "Bulk generated non-fatal exception number $outerCount.$innerCount"
                            )
                        )
                    }
                }
            }
            threads.forEach(Thread::start)
        }
    }
}
