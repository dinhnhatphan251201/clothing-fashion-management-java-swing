package clothing.management.dao;

import clothing.management.entity.LoaiSanPham;
import clothing.management.entity.NhaCungCap;
import clothing.management.entity.SanPham;
import com.google.gson.Gson;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import org.bson.Document;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class SanPhamDao extends AbstractDao {
    private static final Gson GSON = new Gson();
    private MongoCollection<Document> sanPhamCollection;

    public SanPhamDao(MongoClient client) {
        super(client);
        sanPhamCollection = db.getCollection("danhsachsanpham");
    }

    public List<SanPham> layDanhSachSanPham() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<SanPham> dSSanPham = new ArrayList<SanPham>();
        sanPhamCollection.find().subscribe(new Subscriber<Document>() {

            private Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                this.s.request(1);
            }

            @Override
            public void onNext(Document t) {
                String json = t.toJson();
                SanPham sanPham = GSON.fromJson(json, SanPham.class);
                NhaCungCap maNCC = new NhaCungCap(t.getString("maNhaCungCap"));
                LoaiSanPham maLSP = new LoaiSanPham(t.getString("maLoai"));
                sanPham.setLoaiSanPham(maLSP);
                sanPham.setNhaCungCap(maNCC);
                dSSanPham.add(sanPham);
                this.s.request(1);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                onComplete();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        latch.await();
        return dSSanPham;
    }

    public boolean themSanPham(SanPham sanPham) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean rs = new AtomicBoolean(false);
        String json = GSON.toJson(sanPham);
        Document doc = Document.parse(json);
        ////////////
        Document nhaCC = (Document) doc.get("nhaCungCap");
        doc.remove("nhaCungCap");
        doc.append("maNhaCungCap", nhaCC.getString("maNhaCungCap"));
        ///
        Document maLoai = (Document) doc.get("loaiSanPham");
        doc.remove("loaiSanPham");
        doc.append("maLoai", maLoai.getString("maLoai"));

        Publisher<InsertOneResult> pub = sanPhamCollection.insertOne(doc);
        Subscriber<InsertOneResult> sub = new Subscriber<InsertOneResult>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);

            }

            @Override
            public void onNext(InsertOneResult t) {
                if (t.getInsertedId() != null)
                    rs.set(true);

            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                onComplete();

            }

            @Override
            public void onComplete() {
                latch.countDown();

            }
        };
        pub.subscribe(sub);
        latch.await();
        return rs.get();
    }

    public boolean xoaSanPham(String maSanPham) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean rs = new AtomicBoolean(false);
        sanPhamCollection.deleteOne(Filters.eq("maSanPham", maSanPham)).subscribe(new Subscriber<DeleteResult>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);

            }

            @Override
            public void onNext(DeleteResult t) {
                if (t.getDeletedCount() != 0)
                    rs.set(true);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                onComplete();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        latch.await();
        return rs.get();
    }

    public boolean capNhatLoaiSanPham(String maSanPham, SanPham sanPhamMoi) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean rs = new AtomicBoolean(false);
        String json = GSON.toJson(sanPhamMoi);
        Document doc = Document.parse(json);
        Document nhaCC = (Document) doc.get("nhaCungCap");
        doc.remove("nhaCungCap");
        doc.append("maNhaCungCap", nhaCC.getString("maNhaCungCap"));
        ///
        Document maLoai = (Document) doc.get("loaiSanPham");
        doc.remove("loaiSanPham");
        doc.append("maLoai", maLoai.getString("maLoai"));

        System.out.println(doc);
        sanPhamCollection.replaceOne(Filters.eq("maSanPham", maSanPham), doc).subscribe(new Subscriber<UpdateResult>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(UpdateResult t) {
                if (t.getUpsertedId() != null)

                    rs.set(true);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        latch.await();
        return rs.get();
    }

    public List<SanPham> timSanPhamTheoNhaCungCap(String maNhaCungCap) throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);
        List<SanPham> dsSanPham = new ArrayList<SanPham>();
        Document doc = Document.parse("{'maNhaCungCap':'" + maNhaCungCap + "'}");
        sanPhamCollection.find(doc).subscribe(new Subscriber<Document>() {
            private Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                this.s.request(1);
            }

            @Override
            public void onNext(Document t) {

                String json = t.toJson();

                NhaCungCap nhaCC = new NhaCungCap(t.getString("maNhaCungCap"));
                LoaiSanPham loaiSP = new LoaiSanPham(t.getString("maLoai"));
                SanPham sanPham = GSON.fromJson(json, SanPham.class);

                sanPham.setNhaCungCap(nhaCC);
                sanPham.setLoaiSanPham(loaiSP);
                dsSanPham.add(sanPham);

                this.s.request(1);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                onComplete();

            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        latch.await();

        return dsSanPham;
    }

    public List<SanPham> timSanPhamTheoTieuChi(String maSanPham, String tukhoa) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        List<SanPham> dsSanPham = new ArrayList<>();
        Document doc = Document.parse("{'" + maSanPham + "':'" + tukhoa + "'}");

        sanPhamCollection.find(doc).subscribe(new Subscriber<Document>() {
            private Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                this.s.request(1);
            }

            @Override
            public void onNext(Document t) {
//				System.out.println(t);
                String json = t.toJson();
                SanPham sanPham = GSON.fromJson(json, SanPham.class);
                NhaCungCap maNCC = new NhaCungCap(t.getString("maNhaCungCap"));
                LoaiSanPham maLSP = new LoaiSanPham(t.getString("maLoai"));
                sanPham.setLoaiSanPham(maLSP);
                sanPham.setNhaCungCap(maNCC);

                dsSanPham.add(sanPham);
                this.s.request(1);

            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onComplete() {
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
        return dsSanPham;
    }
}
