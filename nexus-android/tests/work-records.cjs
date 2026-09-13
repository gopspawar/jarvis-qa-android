// Exercise the actual work-record functions with an in-memory DOM/storage adapter.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync(__dirname+'/../app/src/main/assets/index.html','utf8');
const js=source.slice(source.indexOf('const wrEl='),source.indexOf('/* ═══ INIT ═══ */'));
const nodes={},storage=new Map();let nextId=100;
const node=()=>({value:'',hidden:false,textContent:'',children:[],style:{},appendChild(c){this.children.push(c)},replaceChildren(){this.children=[]},focus(){},reset(){},reportValidity(){return true}});
for(const id of ['workForm','wrId','wrDate','wrTitle','wrProject','wrStatus','wrHours','wrDetails','wrSearch','wrFilter','wrSummary','workRecordsList'])nodes[id]=node();
const context={S:{workRecords:[]},document:{getElementById:id=>nodes[id],createElement:node},localStorage:{setItem:(k,v)=>storage.set(k,v)},td:()=> '2026-09-13',gid:()=>++nextId,updWorkBadge(){},showNotif(){},alert(){throw Error('Unexpected alert')},confirm:()=>true};
vm.createContext(context);vm.runInContext(js,context);
Object.assign(nodes.wrTitle,{value:'Regression <script>unsafe</script>'});nodes.wrProject.value='NEXUS';nodes.wrDate.value='2026-09-13';nodes.wrStatus.value='In progress';nodes.wrHours.value='2.5';nodes.wrDetails.value='Completed test execution';
context.saveWorkRecord();assert.equal(context.S.workRecords.length,1);assert.equal(JSON.parse(storage.get('nx_workRecords'))[0].hours,2.5);
// Simulate a restart by restoring persisted JSON.
context.S.workRecords=JSON.parse(storage.get('nx_workRecords'));context.renderWorkRecords();assert.equal(nodes.workRecordsList.children.length,1);assert.equal(nodes.workRecordsList.children[0].children[0].textContent,'Regression <script>unsafe</script>');
context.editWorkRecord(101);nodes.wrStatus.value='Completed';context.saveWorkRecord();assert.equal(context.S.workRecords.length,1);assert.equal(context.S.workRecords[0].status,'Completed');
nodes.wrFilter.value='Blocked';context.renderWorkRecords();assert.equal(nodes.workRecordsList.children.length,0);
nodes.wrFilter.value='';nodes.wrSearch.value='nexus';context.renderWorkRecords();assert.equal(nodes.workRecordsList.children.length,1);
context.deleteWorkRecord(101);assert.equal(JSON.parse(storage.get('nx_workRecords')).length,0);
assert.equal((source.match(/'alarms','workRecords'/g)||[]).length,3,'workRecords in load/save/import');
assert(source.includes("const SARVAM_KEY='';"));
console.log('PASS: work add/edit/delete, persisted reload, filters, literal text rendering, persistence hooks and key removal');
