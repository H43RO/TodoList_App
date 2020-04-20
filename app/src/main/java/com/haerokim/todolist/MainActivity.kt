package com.haerokim.todolist

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.haerokim.todolist.databinding.ActivityMainBinding
import com.haerokim.todolist.databinding.ItemTodoBinding

class MainActivity : AppCompatActivity() {
    private val RC_SIGN_IN = 1000
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //로그인이 안됨
        if (FirebaseAuth.getInstance().currentUser == null) {
            login()
        }

        binding.recylcerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)

            //to의-do 객체의 주소값이 왔다갔다해서 편하게 뷰를 조작할 수 있음 (객체지향 프로그래밍 특성 활용)
            adapter = TodoAdapter(
                emptyList(),
                onClickDeleteIcon = {
                    viewModel.deleteTodo(it)
                },
                onClickItem = {
                    viewModel.toggleTodo(it)
                })
        }

        binding.addButton.setOnClickListener {
            //EditText가 비어져있으면 경고 메세지 출력
            if (binding.editText.text.toString() != "") {
                val todo = Todo(binding.editText.text.toString())
                viewModel.addTodo(todo)

            } else {
                Toast.makeText(this, "내용을 입력햊세요", Toast.LENGTH_LONG).show()
            }
        }

        // 관찰 UI Update
        viewModel.todoLiveData.observe(this, Observer {
            (binding.recylcerView.adapter as TodoAdapter).setData(it) //변경된 최신 데이터 it 넣어줌
        })
    }

    //로그인 처리에 대한 결과를 여기서 받음 (RC_SIGN_IN == 1000)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                //로그인 성공
                val user = FirebaseAuth.getInstance().currentUser
            } else {
                //로그인 실패 시 액티비티 종료해버림
                finish()
            }
        }
    }

    fun login() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    //로그아웃 메소드
    fun logout() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                login()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.action_log_out -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

data class Todo(
    val text: String,
    var isDone: Boolean = false
) //기본값 false

class TodoAdapter(
    private var myDataset: List<Todo>,
    val onClickDeleteIcon: (todo: Todo) -> Unit,
    val onClickItem: (todo: Todo) -> Unit
) :
    RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    class TodoViewHolder(val binding: ItemTodoBinding) :
        RecyclerView.ViewHolder(binding.root) //RecyclerView에 붙일 view를 얻음

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TodoAdapter.TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)

        return TodoViewHolder(ItemTodoBinding.bind(view))
    }

    //Item View를 어떻게 보여줄지 결정하는 뷰 홀더
    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = myDataset[position]

        holder.binding.todoText.text = todo.text

        if (todo.isDone) {
            //람다 형식으로 코드를 줄일 수 있다.
            //holder.binding.todoText.paintFlags = holder.binding.todoText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.binding.todoText.apply {
                paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                setTypeface(null, Typeface.ITALIC)
            }
        } else {
            holder.binding.todoText.apply {
                paintFlags = 0
                setTypeface(null, Typeface.NORMAL)
            }
        }

        holder.binding.deleteImageView.setOnClickListener {
            //Adapter를 사용하고 있는 Activity쪽에 Delete 버튼이 눌림을 알려야함
            onClickDeleteIcon.invoke(todo) //눌렀을 때 to-do 전달
        }

        holder.binding.root.setOnClickListener {
            onClickItem.invoke(todo)
        }
    }

    override fun getItemCount() = myDataset.size

    //데이터가 변경될 때마다 notifyDataSetChanged() 호출
    fun setData(newData: List<Todo>) { //LiveData를 보고 데이터 다시 세팅해주는 메소드
        myDataset = newData
        notifyDataSetChanged()
    }
}

//화면 회전 시 액티비티 데이터가 날아가는 현상 픽스 (Activity Life Cycle 문제)
//ViewModel이 해당 액티비티를 관리하도록 할 것임 (데이터 관리하는 요소들도 여기서 처리할 것)

class MainViewModel : ViewModel() {

    val db = Firebase.firestore //데이터 베이스 객체

    //상태 변경, 관찰이 가능한 LiveData (Mutable) 를 저장할 객체 todoLiveData
    //LiveData를 이용하여 상태 관리를 하게 되면 코드가 훨씬 간결해진다
    val todoLiveData = MutableLiveData<List<Todo>>()

    init {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            db.collection(user.uid) //error entity : e
                .addSnapshotListener { value, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }
                    for (document in value!!) { //작성한 컬렉션 문서들 모두 읽어들임
                        //Firebase에서 작성한 컬렉션 문서에서 key 값을 통해 데이터를 가져옴 (Casting 필수)
                        val todo = Todo(
                            document.getString("text")!!,
                            document.getBoolean("isDone")!!
                        )
                        data.add(todo)
                    }
                    todoLiveData.value = data //LiveData에 읽어온 데이터베이스의 데이터값 넣음
                }
        }

    }


    private val data = arrayListOf<Todo>()

    fun addTodo(todo: Todo) {
        data.add(todo)
        todoLiveData.value = data //변경된 최신 데이터를 집어넣음
    }

    fun deleteTodo(todo: Todo) {
        data.remove(todo)
        todoLiveData.value = data //변경된 최신 데이터를 집어넣음
    }

    fun toggleTodo(todo: Todo) {
        todo.isDone = !todo.isDone
        todoLiveData.value = data //변경된 최신 데이터를 집어넣음
    }
}