package com.example.changedetection

import android.app.Activity
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Toast
import es.dmoral.toasty.Toasty


//val colortheme = 0xff4CAF50.toInt()
val colortheme = 0xffffffff.toInt()

class DataProgramacao(var titulo: String, var hora: String, var tipo: String)
class Principal_Item(var titulo: String?, var icone: Int)
class Separador(var titulo: String)
class SeparadorBig(var titulo: String, var subtitulo: String)
class Versiculos2(var temimagem: Boolean, var genre: String?, var year: CharSequence?)
class Estudo_Item(var titulo: String?, var genre: String?, var year: CharSequence?)
class Nucleos_item(
    val hora: String,
    val nome: String,
    val index: String,
    val coordenador: String,
    val preletor: String,
    val hospedeiro: String,
    val zap: String
)

class Ebd_item(val titulo: String, val subtitulo: String, val link: String, val index: String)
class escalas_item(var titulo: String, var subtitulo: String)
class oneStringDiff_copy(var titulo: String)
class oneStringDiff_copy2(var titulo: String)


object OhCrap {
    fun showSuccessToast(act: Activity, str: String) {
        Toasty.success(act, str, Toast.LENGTH_LONG, true).show()
    }

    fun showErrorToast(act: Activity, str: String) {
        Toasty.error(act, str, Toast.LENGTH_LONG, true).show()
    }

    fun showErrorToast(act: Activity) {
        Toasty.error(act, "Erro. Verifique a conexão da internet.", Toast.LENGTH_LONG, true).show()
    }

    fun showErrorToast(act: View?) {
        if (act != null) {
            Toasty.error(
                act.context,
                "Erro. Verifique a conexão da internet.",
                Toast.LENGTH_LONG,
                true
            ).show()
        }
    }
}

object DebugVisibleLogs {
    var isDebug = false

    fun showSuccessToast(act: Activity, str: String) {
        if (isDebug) {
            Toasty.success(act, str, Toast.LENGTH_LONG, true).show()
        }
    }

    fun showErrorToast(act: Activity, str: String) {
        if (isDebug) {
            Toasty.error(act, str, Toast.LENGTH_LONG, true).show()
        }
    }
}


object ClassHelper {
    fun createEbdRecycler(ctx: Activity): RecyclerView {
        val recyclerView: RecyclerView = ctx.findViewById(R.id.defaultRecycler)
        recyclerView.layoutManager = LinearLayoutManager(ctx)

        val draw = ContextCompat.getDrawable(ctx, R.drawable.divider2)
        if (draw != null) {
            val div = DividerItemDecoration(ctx, DividerItemDecoration.VERTICAL)
            div.setDrawable(draw)
            recyclerView.addItemDecoration(div)
        }

        recyclerView.isNestedScrollingEnabled = false

        return recyclerView
    }
}

object BottomSheet {
    fun showSheet(activity: Activity, name: String) {
//        val bottomSheet = activity.findViewById<BottomSheetLayout>(R.id.bottomsheet)
//        val lay = LayoutInflater.from(activity).inflate(R.layout.rowdrigo_picture, bottomSheet, false)
//        val fast2: FastItemAdapter<Cadastrados_Sheet_FA> = FastItemAdapter()
//
//        val recyclerView: RecyclerView = lay.findViewById(R.id.recycler_rodrigo)
//        recyclerView.layoutManager = LinearLayoutManager(activity)
//        recyclerView.adapter = fast2
//
//        val decoration = DividerItemDecoration(recyclerView.context,
//                DividerItemDecoration.VERTICAL)
//        decoration.setDrawable(ContextCompat.getDrawable(activity, R.drawable.divider)!!)
//        recyclerView.addItemDecoration(decoration)
//
//        fast2.add(Cadastrados_Sheet_FA(name, 0))
//
//        fast2.withEventHook(object : ClickEventHook<Cadastrados_Sheet_FA>() {
//            override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
//                return when (viewHolder) {
//                    is Cadastrados_Sheet_FA.ViewHolder0 -> viewHolder.imageView3
//                    else -> null
//                }
//            }
//
//            override fun onClick(v: View, position: Int, fastAdapter: FastAdapter<Cadastrados_Sheet_FA>, item: Cadastrados_Sheet_FA) {
//                val openestudos = Intent(activity, EditarPessoaActivity::class.java)
//                openestudos.putExtra("rua", item.rua)
//                openestudos.putExtra("info", fast2.adapterItems as Serializable)
//                bottomSheet.dismissSheet()
//                activity.startActivity(openestudos)
//            }
//        })
//
//        fast2.withOnClickListener { v, adapter, item, position ->
//            if (item.kind in 2..89) {
//                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                val clip = ClipData.newPlainText(null, item.rua)
//                clipboard.primaryClip = clip
//
//                Toast.makeText(activity, "Copiado", Toast.LENGTH_LONG).show()
//            }
//            true
//        }
//
//        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
//        val myRef: DatabaseReference = database.getReference("v1/private/pessoas_info/" + name)
//        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                var isEmpty = true
//
//                if (dataSnapshot.hasChild("Phone")) {
//                    isEmpty = false
//                    fast2.add(Cadastrados_Sheet_FA("Contato", 1))
//                    dataSnapshot.child("Phone").children.forEach { fast2.add(Cadastrados_Sheet_FA(it.key.toString(), it.value.toString(), 2)) }
//                }
//                if (dataSnapshot.hasChild("Email")) {
//                    isEmpty = false
//                    fast2.add(Cadastrados_Sheet_FA("Email", "", 1))
//                    dataSnapshot.child("Email").children.forEach { fast2.add(Cadastrados_Sheet_FA(it.key.toString().replace("__dot__", "."), it.value.toString(), 3)) }
//                }
//                if (dataSnapshot.hasChild("Endereco")) {
//                    isEmpty = false
//                    fast2.add(Cadastrados_Sheet_FA("Endereço", "", 1))
//                    dataSnapshot.child("Endereco").children.forEach { fast2.add(Cadastrados_Sheet_FA(it.key.toString(), it.value.toString(), 4)) }
//                }
//                if (dataSnapshot.hasChild("Filhos")) {
//                    isEmpty = false
//                    fast2.add(Cadastrados_Sheet_FA("Filhos", "", 1))
//                    dataSnapshot.child("Filhos").children.forEach { fast2.add(Cadastrados_Sheet_FA(it.key.toString(), it.value.toString(), 5)) }
//                }
//                if (dataSnapshot.hasChild("Igreja")) {
//                    isEmpty = false
//                    fast2.add(Cadastrados_Sheet_FA("Igreja", "", 1))
//                    dataSnapshot.child("Igreja").children.forEach { fast2.add(Cadastrados_Sheet_FA(it.value.toString(), it.value.toString(), 6)) }
//                }
//
//                if (isEmpty) {
//                    fast2.add(Cadastrados_Sheet_FA("", "", 98))
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                // Failed to read value
//            }
//        })
//
//        bottomSheet.showWithSheetView(lay)
//        bottomSheet.expandSheet()
    }
}


