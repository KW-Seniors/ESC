package kr.co.kw_seniors.endsemesterclock

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.google.android.material.tabs.TabLayout
import kr.co.kw_seniors.endsemesterclock.databinding.ActivityNoticeBinding
import org.jsoup.Jsoup

class NoticeActivity : AppCompatActivity() {

    companion object{
        const val KW_URL = "https://www.kw.ac.kr"
        /* 일반: 0, 학사: 1, 학생: 2, 등록/장학: 4 */
        // 각 공지사항의 1페이지 주소 = PAGE1_FRONT_BASE_URL + 카테고리 넘버 + PAGE1_BACK_BASE_URL
        // 예: https://www.kw.ac.kr/ko/life/notice.jsp?srCategoryId=0&mode=list&searchKey=1&searchVal= - 일반 공지의 1페이지
        const val PAGE1_FRONT_BASE_URL = "https://www.kw.ac.kr/ko/life/notice.jsp?srCategoryId="
        const val PAGE1_BACK_BASE_URL = "&mode=list&searchKey=1&searchVal="
        // 2페이지 이후 주소 = AFTER_PAGE2_FRONT_BASE_URL + 페이지 넘버 + AFTER_PAGE2_BACK_BASE_URL + 카테고리 넘버
        // 예: https://www.kw.ac.kr/ko/life/notice.jsp?MaxRows=10&tpage=3&searchKey=1&searchVal=&srCategoryId=4 - 등록/장학 공지의 3페이지
        const val AFTER_PAGE2_FRONT_BASE_URL = "https://www.kw.ac.kr/ko/life/notice.jsp?MaxRows=10&tpage="
        const val AFTER_PAGE2_BACK_BASE_URL = "&searchKey=1&searchVal=&srCategoryId="
        // 가져올 페이지 수
        const val MAX_PAGE = 3
        // HTML 문서 내에서 공지사항 아이템 태그의 경로
        const val ITEM_ROUTE = "div.notice div.list-box div.board-list-box ul li div"

        /* 탭 클릭 시 가져올 데이터 */
        const val COMMON_TAB = "[일반]"
        const val BACHELOR_TAB = "[학사]"
        const val STUDENT_TAB = "[학생]"
        const val ENROLL_TAB = "[등록/장학]"
        const val MY_TAB = "즐겨찾기"
    }
    // 레이아웃 바인딩
    val binding by lazy{ActivityNoticeBinding.inflate(layoutInflater)}
    // RoomHelper
    var helper: RoomHelper? = null
    // 리사이클러 뷰 어댑터
    lateinit var adapter: NoticeRecyclerAdapter
    // 수정할 데이터를 임시 저장할 프로퍼티
    var updateNotice: NoticeItem? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // RoomHelper 생성
        helper = Room.databaseBuilder(this, RoomHelper::class.java, "notice_item")
            .allowMainThreadQueries().build()
        adapter = NoticeRecyclerAdapter()
        adapter.helper = helper
        adapter.noticeActivity = this

        // 웹 크롤링
        if(MainActivity.NoticeCrawling==0) {
            helper?.noticeItemDAO()?.clearAll()     // room db data 삭제
            for (category in arrayOf(0, 1, 2, 4)) { // 각 카테고리에 대해,
                for (page in 1..MAX_PAGE) { // 10 페이지 만큼,
                    var thread: Thread
                    if (page == 1) { // 각 공지사항 페이지의 1페이지만 주소가 다름
                        thread = Thread(
                            UrlRun(
                                PAGE1_FRONT_BASE_URL + "$category" + PAGE1_BACK_BASE_URL,
                                page,
                                applicationContext
                            )
                        )
                    } else { // 2페이지 이후로는 주소 형식이 같음 (페이지만 변화)
                        thread = Thread(
                            UrlRun(
                                AFTER_PAGE2_FRONT_BASE_URL + "$page" + AFTER_PAGE2_BACK_BASE_URL + "$category",
                                page,
                                applicationContext
                            )
                        )
                    }
                    // 스레드 실행
                    thread.start()
                    // 웹 크롤링 스레드가 끝날 때까지 메인 스레드 대기
                    thread.join()
                }
            }
            Log.d("NoticeActivity/OnCreate", "웹 크롤링 완료")
            MainActivity.NoticeCrawling = 1
        }
        // 출력될 데이터
        var data: MutableList<NoticeItem>

        // TODO: 처음 시작 시 출력되는 데이터
        data = loadData(COMMON_TAB)
        setData(data)

        // TODO: 탭 리스너 - Room에서 가져올 데이터 지정
        binding.tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when(tab?.position){
                    0 -> {
                        data = loadData(COMMON_TAB)
                    }
                    1 -> {
                        data = loadData(BACHELOR_TAB)
                    }
                    2 -> {
                        data = loadData(STUDENT_TAB)
                    }
                    3 -> {
                        data = loadData(ENROLL_TAB)
                    }
                    4 -> {
                        data = loadData(MY_TAB)
                    }
                }
                setData(data)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })


    }

    private fun setData(data: List<NoticeItem>){
        // adapter = NoticeRecyclerAdapter()
        // adapter.listData.addAll(data?:listOf())
        adapter.listData.clear()
        adapter.listData.addAll(data)
        adapter.notifyDataSetChanged()
        binding.recyclerViewNotice.adapter = adapter
        binding.recyclerViewNotice.layoutManager = LinearLayoutManager(this)
    }

    /* Non-used
    // 탭 레이아웃 프래그먼트 교체 메서드
    private fun replaceView(tab: Fragment){
        var selectedFragment: Fragment? = null
        selectedFragment = tab
        selectedFragment?.let{
            supportFragmentManager.beginTransaction().replace(R.id.frameLayout, it).commit()
        }
    }
     */

    // Room에 저장된 아이템 리스트 불러오기
    private fun loadData(category: String): MutableList<NoticeItem>{
        var data: MutableList<NoticeItem> = mutableListOf()
        // TODO: Room의 데이터 가져오기
        if (category == MY_TAB){
            data = helper?.noticeItemDAO()?.getFavoriteData()!!
        }else {
            data = helper?.noticeItemDAO()?.getCategoryData(category)!!
        }

        return data
    }


    // 웹 크롤링 스레드 클래스
    inner class UrlRun(var url: String, var pages: Int, var context: Context): Runnable{
        // TODO: 아이템 양식 정의
        // lateinit var items: Items
        @Synchronized
        override fun run() {
            try{
                // html 문서 가져오기
                val noticeHtml = Jsoup.connect(url).get()
                // 공지사항 아이템들 가져오기
                val items = noticeHtml.select(ITEM_ROUTE)
                // 가져온 아이템들을 양식에 맞게 저장
                // 가져올 정보
                var url: String      // 주소
                var category: String // 카테고리
                var title: String    // 제목
                var info: String     // 정보
                for (item in items){
                    // url, category, title, info 파싱
                    url = KW_URL + item.select("a").attr("href")
                    category = item.select("strong.category").text()
                    title = item.select("a").text().split("]").last()
                    info = item.select("p.info").text()
                    val noticeItem = NoticeItem(url, category, title, info, "false")
                    helper?.noticeItemDAO()?.insert(noticeItem)

                    Log.i("NoticeActivity/UrlRun", "$url, $category, $title, $info, 'false'")

                }

            }
            catch(e: Exception){
                Log.e("NoticeActivity/UrlRun", e.toString())
            }
        }

    }
}