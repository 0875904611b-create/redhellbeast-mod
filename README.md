# Red: The Hellbeast  (Forge 1.20.1 / 47.3.0, Java 17)

บอส **Red** จาก creepypasta "NES Godzilla" สำหรับ Minecraft Forge 1.20.1

> ⚠️ นี่คือ **ซอร์สโค้ดโปรเจกต์** ยังไม่ได้ build เป็น .jar และยังไม่เคยทดสอบในเกมจริง
> (ผู้เขียนไม่มีเน็ตและไม่มี Minecraft/Forge ตอนสร้าง) ถ้า build แล้วมี error ส่งข้อความ error มาให้แก้ต่อได้

## วิธี build (ง่ายที่สุด)
1. ดาวน์โหลด **Forge MDK 1.20.1-47.3.0** จาก https://files.minecraftforge.net/ แล้วแตก zip
2. ลบโฟลเดอร์ `src` ของ MDK ทิ้ง แล้ว **คัดลอกโฟลเดอร์ `src` ของโปรเจกต์นี้ไปแทน**
3. เปิด `build.gradle` ของ MDK -> ตั้ง `archivesName = 'redhellbeast'` และ `version = '1.0.0'`
   (หรือจะใช้ build.gradle ในโปรเจกต์นี้ทับเลยก็ได้ ถ้าใช้ gradle wrapper ของ MDK)
4. เปิดเทอร์มินัลในโฟลเดอร์ MDK แล้วรัน  `./gradlew build`  (Windows: `gradlew.bat build`)
5. ได้ไฟล์ mod ที่ `build/libs/*.jar` -> ใส่ในโฟลเดอร์ `mods` ของ Forge 1.20.1
ทดสอบเร็วๆ ใน IDE ได้ด้วย `./gradlew runClient`

## วิธีเล่น
1. หา **แผ่นเกม Godzilla** (Godzilla: Monster of Monsters! Cartridge) — มีโอกาส **35%** ในหีบทุกใบ
   (หมู่บ้าน ปราสาทเนเทอร์ ป้อม ดันเจี้ยน ฯลฯ ทุก loot table ที่ขึ้นต้น `chests/`)
2. คราฟต์ **Retro Computer**: เหล็ก 6 + แผ่นกระจก 1 + เรดสโตน 1
   ```
   I I I
   I G I      I = iron ingot, G = glass pane, R = redstone
   I R I
   ```
3. วางคอม แล้ว **คลิกขวาด้วยแผ่นเกม** → จอขึ้น GAME OVER → ตัวอักษรเพิ่มขึ้นเรื่อยๆ จนเป็นรูป Red → jumpscare → ถูกดูดเข้า dimension "NES realm"
4. สู้บอสในสนาม (ขุด/วางบล็อกไม่ได้) ชนะแล้วจะถูกส่งกลับที่เดิม พร้อมรางวัล

ไข่เสก Red มีใน Creative tab "Red: The Hellbeast" (ใช้เทสได้ ควรเล่นระดับ Easy ขึ้นไป — Peaceful บอสไม่ทำดาเมจ)

## บอส 3 เฟส (700 HP)
**เฟส 1 – The Hellbeast (100→66%)**  Claw • Lava Fingers (เส้นไฟระเบิดตามพื้น) • Soul Burn (ดึงตัว+เผา+Wither) • Glitch Step (วาร์ปไปหลังคุณ) • พื้นกลายเป็นลาวาเป็นหย่อมๆ
**เฟส 2 – Corrupted (66→33%)**  เพิ่ม Hellfire Skulls (ตีสะท้อนกลับได้!) • เรียก Glitch Sprites • Hell Rain (ไฟตกใส่ทั่วสนาม) • ลาวามากขึ้น เร็วขึ้น
**เฟส 3 – GAME OVER (33→0%)**  เพิ่ม Hell Beam (เลเซอร์ไล่ตาม ใช้เสาบังได้) • CRASH (คลื่นกระแทกวงแหวน **กระโดดข้าม**) • ตีสองที • หน้าจอ glitch ตลอด
ระหว่างเปลี่ยนเฟสบอสอมตะ ~4 วินาที พร้อมเอฟเฟกต์ glitch/สั่นกล้อง

## ปรับแต่ง
* เปลี่ยนโอกาสดรอป: `src/main/resources/data/redhellbeast/loot_modifiers/cartridge_in_chests.json` → `"chance": 0.35`
* ดาเมจ/เลือดบอส: `RedEntity.createAttributes()` และตัวเลขในเมธอด `tickAttack`
* ภาษา: `assets/redhellbeast/lang/` (en_us, th_th)

## เครดิต/หมายเหตุ
* เรื่องต้นฉบับ: NES Godzilla Creepypasta โดย Cosbydaf (แฟนเมดนี้ไม่เกี่ยวข้องกับ Toho/Nintendo)
* texture jumpscare ใช้ sprite ที่ผู้ใช้ส่งมา; texture อื่นสร้างด้วยสคริปต์ (`gen_assets.py`)
