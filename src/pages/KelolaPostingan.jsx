import { useEffect, useState } from "react";

export default function KelolaPostingan({ user }) {
  const idPenjual = user?.idUser || "USR001";
  const [daftar, setDaftar] = useState([]);
  const [bukaForm, setBukaForm] = useState(false);
  const [modeEdit, setModeEdit] = useState(false);
  const [pesan, setPesan] = useState("");
  const [form, setForm] = useState({
    idPostingan: "",
    judul: "",
    kategori: "Transportasi",
    harga: "",
    jamTayang: "",
    deskripsi: "",
  });

  const muatData = async () => {
    const res = await fetch(`/api/postingan/penjual/${idPenjual}`);
    const json = await res.json();
    if (json.berhasil) setDaftar(json.data || []);
  };

  useEffect(() => {
    muatData();
  }, [idPenjual]);

  const bukaBuat = () => {
    setModeEdit(false);
    setForm({ idPostingan: "", judul: "", kategori: "Transportasi", harga: "", jamTayang: "", deskripsi: "" });
    setBukaForm(true);
  };

  const bukaEdit = (item) => {
    setModeEdit(true);
    setForm({
      idPostingan: item.idPostingan,
      judul: item.judul,
      kategori: item.kategori,
      harga: item.harga,
      jamTayang: item.jamTayang?.slice(0, 16) || "",
      deskripsi: item.deskripsi,
    });
    setBukaForm(true);
  };

  const simpan = async (e) => {
    e.preventDefault();
    const url = modeEdit ? `/api/postingan/${form.idPostingan}` : "/api/postingan/buat";
    const method = modeEdit ? "PUT" : "POST";

    const res = await fetch(url, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        id_penjual: idPenjual,
        judul: form.judul,
        kategori: form.kategori,
        harga: Number(form.harga),
        jam_tayang: form.jamTayang.length === 16 ? `${form.jamTayang}:00` : form.jamTayang,
        deskripsi: form.deskripsi,
      }),
    });
    const json = await res.json();
    setPesan(json.pesan);
    if (json.berhasil) {
      setBukaForm(false);
      muatData();
    }
  };

  const hapus = async (idPostingan) => {
    const res = await fetch(`/api/postingan/${idPostingan}`, {
      method: "DELETE",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ id_penjual: idPenjual }),
    });
    const json = await res.json();
    setPesan(json.pesan);
    if (json.berhasil) muatData();
  };

  return (
    <div className="max-w-6xl mx-auto px-6 py-8">
      <div className="flex flex-wrap justify-between items-center gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-extrabold text-stone-900">Kelola Penawaran Saya</h1>
          <p className="text-sm text-stone-500">Buat, perbarui, atau hapus penawaran jasa dan barang kamu.</p>
        </div>
        <button onClick={bukaBuat} className="bg-[#D85012] hover:bg-[#B8420E] text-white font-bold text-sm px-4 py-2.5 rounded-xl">
          + Buat Postingan
        </button>
      </div>

      {pesan && (
        <div className="mb-5 px-4 py-3 rounded-xl bg-orange-50 border border-orange-200 text-orange-900 text-sm">
          {pesan}
        </div>
      )}

      {bukaForm && (
        <form onSubmit={simpan} className="bg-white border border-stone-200 rounded-2xl p-6 mb-8 shadow-xs">
          <h2 className="text-lg font-bold mb-4">{modeEdit ? "Perbarui Penawaran" : "Buat Penawaran Baru"}</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-stone-600 mb-1">Judul Penawaran</label>
              <input type="text" required maxLength={100} value={form.judul} onChange={(e) => setForm({ ...form, judul: e.target.value })} className="w-full border border-stone-300 rounded-xl px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-xs font-bold text-stone-600 mb-1">Kategori</label>
              <select value={form.kategori} onChange={(e) => setForm({ ...form, kategori: e.target.value })} className="w-full border border-stone-300 rounded-xl px-3 py-2 text-sm bg-white">
                <option value="Transportasi">Transportasi</option>
                <option value="Jasa Titip">Jasa Titip</option>
                <option value="Les dan Bimbingan">Les dan Bimbingan</option>
                <option value="Desain & Kreatif">Desain & Kreatif</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold text-stone-600 mb-1">Harga (Rp)</label>
              <input type="number" required min={1} value={form.harga} onChange={(e) => setForm({ ...form, harga: e.target.value })} className="w-full border border-stone-300 rounded-xl px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="block text-xs font-bold text-stone-600 mb-1">Batas Jam Tayang</label>
              <input type="datetime-local" required value={form.jamTayang} onChange={(e) => setForm({ ...form, jamTayang: e.target.value })} className="w-full border border-stone-300 rounded-xl px-3 py-2 text-sm" />
            </div>
            <div className="md:col-span-2">
              <label className="block text-xs font-bold text-stone-600 mb-1">Deskripsi</label>
              <textarea rows={3} required value={form.deskripsi} onChange={(e) => setForm({ ...form, deskripsi: e.target.value })} className="w-full border border-stone-300 rounded-xl px-3 py-2 text-sm" />
            </div>
          </div>
          <div className="flex gap-2 mt-5">
            <button type="submit" className="bg-[#D85012] text-white font-bold text-sm px-4 py-2 rounded-xl">Simpan</button>
            <button type="button" onClick={() => setBukaForm(false)} className="border border-stone-300 text-stone-700 font-semibold text-sm px-4 py-2 rounded-xl">Batal</button>
          </div>
        </form>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {daftar.map((item) => (
          <div key={item.idPostingan} className="bg-white border border-stone-200 rounded-2xl p-5 flex flex-col justify-between shadow-xs">
            <div>
              <div className="flex justify-between items-center mb-3">
                <span className="bg-[#FDECE3] text-[#C2410C] text-xs font-bold px-3 py-1 rounded-full">{item.kategori}</span>
                <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${item.statusAktif ? "bg-green-100 text-green-700" : "bg-stone-100 text-stone-500"}`}>
                  {item.statusAktif ? "Aktif" : "Nonaktif"}
                </span>
              </div>
              <h3 className="font-bold text-stone-900 text-base mb-1">{item.judul}</h3>
              <p className="text-xs text-stone-500 mb-3">{item.deskripsi}</p>
              <p className="font-extrabold text-[#D85012] text-base mb-1">Rp {Number(item.harga).toLocaleString("id-ID")}</p>
              <p className="text-[11px] text-stone-400 mb-4">Tayang s/d: {item.jamTayang?.replace("T", " ")}</p>
            </div>
            <div className="border-t border-stone-100 pt-3 flex justify-end gap-2">
              <button onClick={() => bukaEdit(item)} className="border border-stone-300 text-stone-700 text-xs font-semibold px-3 py-1.5 rounded-lg">Ubah</button>
              <button onClick={() => hapus(item.idPostingan)} className="bg-red-50 text-red-600 border border-red-200 text-xs font-semibold px-3 py-1.5 rounded-lg">Hapus</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}