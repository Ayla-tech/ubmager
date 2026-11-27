/* Aksi halaman Pemesanan & Pembayaran.
   Semua permintaan tulis diarahkan ke REST API yang sudah ada. */
(function () {
  'use strict';

  var rupiah = new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    maximumFractionDigits: 0
  });
  var formatWaktu = new Intl.DateTimeFormat('id-ID', { dateStyle: 'long', timeStyle: 'short' });

  function pesan(id, jenis, teks) {
    var el = document.getElementById(id);
    if (!el) {
      return;
    }
    el.textContent = teks;
    el.classList.remove('alert-sukses', 'alert-error');
    el.classList.add(jenis === 'sukses' ? 'alert-sukses' : 'alert-error');
    el.hidden = false;
  }

  function bersihkan(id) {
    var el = document.getElementById(id);
    if (!el) {
      return;
    }
    el.textContent = '';
    el.hidden = true;
  }

  function nonaktif(tombol, teks) {
    tombol.dataset.teksAsli = tombol.textContent;
    tombol.disabled = true;
    tombol.textContent = teks;
  }

  function aktifkan(tombol) {
    tombol.disabled = false;
    if (tombol.dataset.teksAsli) {
      tombol.textContent = tombol.dataset.teksAsli;
    }
  }

  function aktifkanLangkah(el, aktif) {
    if (!el) {
      return;
    }
    el.classList.toggle('bg-brand-500', aktif);
    el.classList.toggle('text-white', aktif);
    el.classList.toggle('bg-stone-100', !aktif);
    el.classList.toggle('text-stone-500', !aktif);
  }

  function waktuTampil(nilai) {
    if (!nilai) {
      return '-';
    }
    var waktu = new Date(nilai);
    return Number.isNaN(waktu.getTime()) ? nilai : formatWaktu.format(waktu);
  }

  async function kirimJson(url, metode, isi) {
    var opsi = {
      method: metode,
      credentials: 'same-origin',
      headers: { 'Accept': 'application/json' }
    };
    if (isi !== undefined) {
      opsi.headers['Content-Type'] = 'application/json';
      opsi.body = JSON.stringify(isi);
    }

    var res;
    try {
      res = await fetch(url, opsi);
    } catch (e) {
      throw new Error('Tidak dapat menghubungi server. Periksa koneksimu.');
    }

    var data = null;
    try {
      data = await res.json();
    } catch (e) {
      // Respons bukan JSON.
    }

    if (!res.ok || !data || data.berhasil !== true) {
      throw new Error(data && data.pesan ? data.pesan : 'Permintaan gagal (HTTP ' + res.status + ').');
    }
    return data;
  }

  // ---------- Dialog konfirmasi ----------

  var aksiDialog = null;

  function siapkanDialog() {
    var dialog = document.getElementById('dialogKonfirmasi');
    if (!dialog) {
      return;
    }
    var setuju = document.getElementById('dialogSetuju');
    setuju.addEventListener('click', function (e) {
      e.preventDefault();
      var aksi = aksiDialog;
      aksiDialog = null;
      dialog.close();
      if (aksi) {
        aksi();
      }
    });
    dialog.addEventListener('close', function () {
      aksiDialog = null;
    });
    dialog.addEventListener('cancel', function () {
      aksiDialog = null;
    });
  }

  function konfirmasi(opsi) {
    var dialog = document.getElementById('dialogKonfirmasi');
    if (!dialog || typeof dialog.showModal !== 'function') {
      if (window.confirm(opsi.pesan)) {
        opsi.onSetuju();
      }
      return;
    }

    document.getElementById('dialogJudul').textContent = opsi.judul || 'Konfirmasi';
    document.getElementById('dialogPesan').textContent = opsi.pesan || '';
    var tombol = document.getElementById('dialogSetuju');
    tombol.textContent = opsi.label || 'Ya, lanjutkan';
    tombol.className = opsi.bahaya ? 'btn-danger' : 'btn-primary';
    aksiDialog = opsi.onSetuju;
    dialog.showModal();
  }

  // ---------- Form pemesanan jasa ----------

  function initFormPemesanan() {
    var form = document.getElementById('formPemesanan');
    if (!form) {
      return;
    }

    var harga = Number(form.dataset.harga || '0');
    var idPostingan = form.dataset.idPostingan;
    var idPembeli = form.dataset.idPembeli;
    var jumlah = document.getElementById('jumlahPesanan');
    var waktu = document.getElementById('waktuPesanan');
    var lokasi = document.getElementById('lokasiPesanan');
    var catatan = document.getElementById('catatanPesanan');
    var estimasi = document.getElementById('estimasiTotal');
    var isi = document.getElementById('langkahIsi');
    var ringkasan = document.getElementById('langkahRingkasan');
    var indikatorSatu = document.getElementById('indikatorSatu');
    var indikatorDua = document.getElementById('indikatorDua');
    var tombolUbah = document.getElementById('tombolUbah');
    var tombolKirim = document.getElementById('tombolKirim');

    var sekarang = new Date();
    sekarang.setMinutes(sekarang.getMinutes() - sekarang.getTimezoneOffset());
    waktu.min = sekarang.toISOString().slice(0, 16);

    function total() {
      var n = parseInt(jumlah.value, 10);
      return harga * (Number.isInteger(n) && n > 0 ? n : 0);
    }

    function perbaruiEstimasi() {
      estimasi.textContent = rupiah.format(total());
    }

    function validasi() {
      var n = Number(jumlah.value);
      if (!Number.isInteger(n) || n < 1) {
        return 'Jumlah harus berupa bilangan bulat minimal 1.';
      }
      if (!waktu.value) {
        return 'Waktu pelaksanaan wajib diisi.';
      }
      var tanggal = new Date(waktu.value);
      if (Number.isNaN(tanggal.getTime())) {
        return 'Format waktu pelaksanaan tidak dikenali.';
      }
      if (tanggal.getTime() < Date.now() - 60000) {
        return 'Waktu pelaksanaan tidak boleh di masa lalu.';
      }
      if (!lokasi.value.trim()) {
        return 'Lokasi wajib diisi.';
      }
      return null;
    }

    jumlah.addEventListener('input', function () {
      perbaruiEstimasi();
      bersihkan('pesanForm');
    });

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      var masalah = validasi();
      if (masalah) {
        pesan('pesanForm', 'error', masalah);
        return;
      }
      bersihkan('pesanForm');

      document.getElementById('ringkasanJumlah').textContent = String(Number(jumlah.value));
      document.getElementById('ringkasanHarga').textContent = rupiah.format(harga);
      document.getElementById('ringkasanTotal').textContent = rupiah.format(total());
      document.getElementById('ringkasanWaktu').textContent = waktuTampil(waktu.value);
      document.getElementById('ringkasanLokasi').textContent = lokasi.value.trim();
      document.getElementById('ringkasanCatatan').textContent = catatan.value.trim() || '-';

      isi.hidden = true;
      ringkasan.hidden = false;
      aktifkanLangkah(indikatorSatu, false);
      aktifkanLangkah(indikatorDua, true);
      ringkasan.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    });

    tombolUbah.addEventListener('click', function () {
      ringkasan.hidden = true;
      isi.hidden = false;
      aktifkanLangkah(indikatorSatu, true);
      aktifkanLangkah(indikatorDua, false);
      jumlah.focus();
    });

    tombolKirim.addEventListener('click', async function () {
      var masalah = validasi();
      if (masalah) {
        pesan('pesanRingkasan', 'error', masalah);
        return;
      }

      nonaktif(tombolKirim, 'Mengirim pesanan...');
      bersihkan('pesanRingkasan');

      try {
        var data = await kirimJson('/api/pemesanan', 'POST', {
          id_pembeli: idPembeli,
          id_postingan: idPostingan,
          jumlah: Number(jumlah.value),
          waktu_pesanan: waktu.value,
          lokasi: lokasi.value.trim(),
          catatan: catatan.value.trim()
        });
        var dibuat = data.data && data.data.pemesanan && data.data.pemesanan.id_pemesanan;
        if (!dibuat) {
          throw new Error('Respons pemesanan tidak memuat nomor pemesanan.');
        }
        window.location.assign('/pemesanan/' + encodeURIComponent(dibuat));
      } catch (err) {
        pesan('pesanRingkasan', 'error', err.message);
        aktifkan(tombolKirim);
      }
    });
  }

  // ---------- Daftar pesanan masuk (penyedia) ----------

  function initPemesananIndex() {
    var daftar = document.getElementById('daftarPemesananPenyedia');
    if (!daftar) {
      return;
    }
    var idPenyedia = daftar.dataset.idPenyedia;

    daftar.addEventListener('click', function (e) {
      var tombol = e.target.closest('button[data-aksi]');
      if (!tombol || tombol.disabled) {
        return;
      }

      var aksi = tombol.dataset.aksi;
      var id = tombol.dataset.id;
      var judul = tombol.dataset.judul || 'pesanan ini';
      var terima = aksi === 'terima';

      konfirmasi({
        judul: terima ? 'Terima pesanan?' : 'Tolak pesanan?',
        pesan: terima
          ? 'Pesanan "' + judul + '" akan diterima. Pembeli bisa melanjutkan ke pembayaran.'
          : 'Pesanan "' + judul + '" akan ditolak dan dibatalkan. Tindakan ini tidak bisa dibatalkan dari halaman ini.',
        label: terima ? 'Ya, terima' : 'Ya, tolak',
        bahaya: !terima,
        onSetuju: async function () {
          nonaktif(tombol, terima ? 'Menerima...' : 'Menolak...');
          bersihkan('pesanAksi');
          try {
            await kirimJson('/api/pemesanan/' + encodeURIComponent(id) + '/' + aksi, 'PUT', {
              id_penyedia: idPenyedia
            });
            pesan('pesanAksi', 'sukses',
              terima ? 'Pesanan berhasil diterima.' : 'Pesanan berhasil ditolak.');
            setTimeout(function () {
              window.location.reload();
            }, 700);
          } catch (err) {
            pesan('pesanAksi', 'error', err.message);
            aktifkan(tombol);
          }
        }
      });
    });
  }

  // ---------- Detail pemesanan: tandai selesai ----------

  function initPemesananDetail() {
    var tombol = document.getElementById('tombolSelesai');
    if (!tombol) {
      return;
    }
    var id = tombol.dataset.id;
    var idPenyedia = tombol.dataset.idPenyedia;

    tombol.addEventListener('click', function () {
      konfirmasi({
        judul: 'Tandai layanan selesai?',
        pesan: 'Pastikan layanan benar-benar sudah selesai. Status pesanan akan menjadi selesai dan pembeli bisa memberi review.',
        label: 'Ya, selesaikan',
        onSetuju: async function () {
          nonaktif(tombol, 'Menyelesaikan...');
          bersihkan('pesanAksi');
          try {
            await kirimJson('/api/pemesanan/' + encodeURIComponent(id) + '/selesai', 'POST', {
              id_penyedia: idPenyedia
            });
            pesan('pesanAksi', 'sukses', 'Layanan berhasil ditandai selesai.');
            setTimeout(function () {
              window.location.reload();
            }, 700);
          } catch (err) {
            pesan('pesanAksi', 'error', err.message);
            aktifkan(tombol);
          }
        }
      });
    });
  }

  // ---------- Form pembayaran (pembeli) ----------

  function initPembayaranForm() {
    var tombol = document.getElementById('tombolSudahBayar');
    if (!tombol) {
      return;
    }

    var idPesanan = tombol.dataset.idPesanan;
    var radio = Array.prototype.slice.call(document.querySelectorAll('input[name="metode"]'));
    var kartu = Array.prototype.slice.call(document.querySelectorAll('.metode-kartu'));
    var instruksi = document.getElementById('instruksiPembayaran');
    var idPembayaran = null;
    var metode = '';

    function terapkanMetode() {
      kartu.forEach(function (el) {
        var input = el.querySelector('input[name="metode"]');
        var dipilih = input.checked;
        el.classList.toggle('border-brand-400', dipilih);
        el.classList.toggle('bg-brand-50', dipilih);
        el.classList.toggle('ring-2', dipilih);
        el.classList.toggle('ring-brand-200', dipilih);
        el.classList.toggle('border-stone-200', !dipilih);
        el.classList.toggle('bg-white', !dipilih);
      });

      var terpilih = radio.filter(function (r) { return r.checked; })[0];
      metode = terpilih ? terpilih.value : '';
      tombol.disabled = !metode;
      instruksi.hidden = !metode;
      Array.prototype.slice.call(document.querySelectorAll('[data-instruksi]')).forEach(function (p) {
        p.hidden = p.dataset.instruksi !== metode;
      });
    }

    radio.forEach(function (r) {
      r.addEventListener('change', terapkanMetode);
    });
    terapkanMetode();

    tombol.addEventListener('click', function () {
      if (!metode) {
        return;
      }
      konfirmasi({
        judul: 'Konfirmasi pembayaran',
        pesan: 'Pastikan kamu sudah membayar sesuai total tagihan dengan metode ' + metode + '. Status akan menjadi menunggu konfirmasi penyedia.',
        label: 'Ya, saya sudah bayar',
        onSetuju: async function () {
          nonaktif(tombol, 'Mencatat pembayaran...');
          bersihkan('pesanBayar');
          try {
            var data = await kirimJson('/api/pembayaran', 'POST', {
              id_pesanan: idPesanan,
              metode: metode
            });
            idPembayaran = data.data && data.data.pembayaran && data.data.pembayaran.id_pembayaran;
            if (!idPembayaran) {
              throw new Error('Respons pembayaran tidak memuat nomor pembayaran.');
            }

            document.getElementById('nomorPembayaran').textContent = idPembayaran;
            document.getElementById('lewatiBukti').href = '/pembayaran/' + encodeURIComponent(idPembayaran);
            document.getElementById('langkahMetode').hidden = true;
            document.getElementById('langkahBukti').hidden = false;
            aktifkanLangkah(document.getElementById('indikatorSatu'), false);
            aktifkanLangkah(document.getElementById('indikatorDua'), true);
            document.getElementById('buktiPembayaran').focus();
          } catch (err) {
            pesan('pesanBayar', 'error', err.message);
            aktifkan(tombol);
          }
        }
      });
    });

    var formBukti = document.getElementById('formBukti');
    formBukti.addEventListener('submit', async function (e) {
      e.preventDefault();
      var input = document.getElementById('buktiPembayaran');
      var nilai = input.value.trim();

      if (!idPembayaran) {
        pesan('pesanBukti', 'error', 'Catat pembayaran terlebih dahulu sebelum mengirim bukti.');
        return;
      }
      if (!nilai) {
        pesan('pesanBukti', 'error', 'Isi tautan atau path bukti, atau pilih "Kirim nanti".');
        input.focus();
        return;
      }
      if (nilai.length > 255) {
        pesan('pesanBukti', 'error', 'Bukti maksimal 255 karakter.');
        return;
      }

      var kirim = formBukti.querySelector('button[type="submit"]');
      nonaktif(kirim, 'Mengirim bukti...');
      bersihkan('pesanBukti');
      try {
        await kirimJson('/api/pembayaran/' + encodeURIComponent(idPembayaran) + '/bukti', 'POST', {
          bukti_pembayaran: nilai
        });
        window.location.assign('/pembayaran/' + encodeURIComponent(idPembayaran));
      } catch (err) {
        pesan('pesanBukti', 'error', err.message);
        aktifkan(kirim);
      }
    });
  }

  // ---------- Detail pembayaran ----------

  function initPembayaranDetail() {
    var konfirmasiTombol = document.getElementById('tombolKonfirmasiPembayaran');
    var perbaikanTombol = document.getElementById('tombolMintaPerbaikan');
    var selesaiTombol = document.getElementById('tombolSelesaikanLayanan');

    if (konfirmasiTombol) {
      konfirmasiTombol.addEventListener('click', function () {
        var total = konfirmasiTombol.dataset.total || 'tagihan ini';
        konfirmasi({
          judul: 'Konfirmasi pembayaran?',
          pesan: 'Pembayaran sebesar ' + total + ' akan ditandai terkonfirmasi. Pastikan dana benar-benar sudah kamu terima.',
          label: 'Ya, konfirmasi',
          onSetuju: async function () {
            nonaktif(konfirmasiTombol, 'Mengonfirmasi...');
            bersihkan('pesanAksi');
            try {
              await kirimJson('/api/pembayaran/'
                + encodeURIComponent(konfirmasiTombol.dataset.id) + '/konfirmasi', 'PUT', {
                id_penyedia: konfirmasiTombol.dataset.idPenyedia
              });
              pesan('pesanAksi', 'sukses', 'Pembayaran berhasil dikonfirmasi.');
              setTimeout(function () {
                window.location.reload();
              }, 700);
            } catch (err) {
              pesan('pesanAksi', 'error', err.message);
              aktifkan(konfirmasiTombol);
            }
          }
        });
      });
    }

    if (perbaikanTombol) {
      perbaikanTombol.addEventListener('click', function () {
        konfirmasi({
          judul: 'Minta perbaikan bukti?',
          pesan: 'Status pembayaran akan menjadi perlu perbaikan agar pembeli mengirim ulang bukti. Catatan perbaikan belum tersimpan di backend, jadi sampaikan detailnya langsung kepada pembeli.',
          label: 'Ya, minta perbaikan',
          bahaya: true,
          onSetuju: async function () {
            nonaktif(perbaikanTombol, 'Mengirim permintaan...');
            bersihkan('pesanAksi');
            try {
              await kirimJson('/api/pembayaran/'
                + encodeURIComponent(perbaikanTombol.dataset.id) + '/perbaikan', 'PUT', {
                id_penyedia: perbaikanTombol.dataset.idPenyedia
              });
              pesan('pesanAksi', 'sukses', 'Permintaan perbaikan berhasil dikirim.');
              setTimeout(function () {
                window.location.reload();
              }, 700);
            } catch (err) {
              pesan('pesanAksi', 'error', err.message);
              aktifkan(perbaikanTombol);
            }
          }
        });
      });
    }

    if (selesaiTombol) {
      selesaiTombol.addEventListener('click', function () {
        konfirmasi({
          judul: 'Tandai layanan selesai?',
          pesan: 'Pembayaran sudah dikonfirmasi. Setelah layanan benar-benar selesai, tandai pesanan agar pembeli bisa memberi review.',
          label: 'Ya, selesaikan',
          onSetuju: async function () {
            nonaktif(selesaiTombol, 'Menyelesaikan...');
            bersihkan('pesanAksi');
            try {
              await kirimJson('/api/pemesanan/'
                + encodeURIComponent(selesaiTombol.dataset.id) + '/selesai', 'POST', {
                id_penyedia: selesaiTombol.dataset.idPenyedia
              });
              pesan('pesanAksi', 'sukses', 'Layanan berhasil ditandai selesai.');
              setTimeout(function () {
                window.location.reload();
              }, 700);
            } catch (err) {
              pesan('pesanAksi', 'error', err.message);
              aktifkan(selesaiTombol);
            }
          }
        });
      });
    }

    var formBukti = document.getElementById('formBuktiPembayaran');
    if (formBukti) {
      formBukti.addEventListener('submit', async function (e) {
        e.preventDefault();
        var input = document.getElementById('buktiPembayaranBaru');
        var nilai = input.value.trim();
        if (!nilai) {
          pesan('pesanAksi', 'error', 'Isi tautan atau path bukti terlebih dahulu.');
          input.focus();
          return;
        }
        if (nilai.length > 255) {
          pesan('pesanAksi', 'error', 'Bukti maksimal 255 karakter.');
          return;
        }

        var kirim = formBukti.querySelector('button[type="submit"]');
        nonaktif(kirim, 'Mengirim bukti...');
        bersihkan('pesanAksi');
        try {
          await kirimJson('/api/pembayaran/'
            + encodeURIComponent(formBukti.dataset.idPembayaran) + '/bukti', 'POST', {
            bukti_pembayaran: nilai
          });
          pesan('pesanAksi', 'sukses', 'Bukti pembayaran berhasil dikirim.');
          setTimeout(function () {
            window.location.reload();
          }, 700);
        } catch (err) {
          pesan('pesanAksi', 'error', err.message);
          aktifkan(kirim);
        }
      });
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    siapkanDialog();
    initFormPemesanan();
    initPemesananIndex();
    initPemesananDetail();
    initPembayaranForm();
    initPembayaranDetail();
  });
})();
