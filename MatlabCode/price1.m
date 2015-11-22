function price1(N_EV)

%Load Aggregator data
dataName='ADMM_matlab/Aggregator/aggregator.mat';
agr_params= load(dataName); % data required by the agregator

N=N_EV+1;                       % Number of agents
deltaT=15*60;                   % Time slot duration [sec]
T= 24*3600/deltaT ;             % Number of time slots

price = agr_params.price;       %Price
p=repmat(price,4,1);
p=p(:) / (3600*1000) * deltaT;  % scaling of price in EUR/kW
agr_params.p=p;

agr_params.re=   60*ones(T,1);  % maximal aviliable load 1GW 
agr_params.xamin=-100e3*ones(T,1);



xa_max=agr_params.re;

xa_min = agr_params.xamin;
price = agr_params.p;
alpha=0.05/3600 * 15*60  ;
x_max=4;                                 % Max charging power for greedy need to add some 1e-3 or someting for it to be feasible
x_min=-4;                                % Min charging power
rho =0.01;
gamma =0;

%xmean=zeros(T,1);       % Avarage of all agent profiles 96*1
%u = zeros(T,1);
%x = zeros(T,N);
%xold= x;

%Data of master iteration 3. This will generate the output for iteration 4
xmean=[0.5066893470996584 0.5066893470971756 0.5066893470944465 0.5066893470914337 0.481273020471175 0.4812730204674837 0.481273020463483 0.4812730204591208 0.4736322041045641 0.4736322040993001 0.47363220409349865 0.47363220408704887 0.46811383671671225 0.4681138367085282 0.46811383669919043 0.4681138366883645 0.4576607754217622 0.4576607754063845 0.4576607753873839 0.4576607753626972 0.44948934673397006 0.44948934667878354 0.4494893465757239 0.4494893463566537 0.4644229513323377 0.46442295034984865 0.4514909933830909 0.4514909927766736 0.4777074815124355 0.45503328854433783 0.4139520000000001 0.4139520000000001 0.45020800000000005 0.45020800000000005 0.45020800000000005 0.45020800000000005 0.5023039999999999 0.5023039999999999 0.5023039999999999 0.5023039999999999 0.5112800000000001 0.5112800000000001 0.5112800000000001 0.5112800000000001 0.5248320000000002 0.5248320000000002 0.5248320000000002 0.5248320000000002 0.528264 0.528264 0.528264 0.528264 0.4673679999999999 0.4673679999999999 0.4673679999999999 0.4673679999999999 0.4481839999999999 0.4481839999999999 0.4481839999999999 0.4481839999999999 0.447832 0.447832 0.447832 0.447832 0.48461599999999994 0.5171101058454293 0.533096587109883 0.533096587109883 0.6416301382991636 0.6416301382991636 0.6311879323321382 0.631187932334643 0.6539857283400423 0.6539857283424935 0.6539857283449001 0.6539857283454997 0.6157500547424789 0.6157500547429304 0.6157500547433326 0.615750054743677 0.5420910341559331 0.5420910341562097 0.5420910341565192 0.5420910341568644 0.5093603402210682 0.49607710217722467 0.49607710217661605 0.4960771021759509 0.5077505716047926 0.5077505716039504 0.5077505716030253 0.5077505716020113 0.4996852654492784 0.4996852654480889 0.499685265446138 0.499685265444033];
u=[1.795859098876118 1.795859098970463 1.7958590990684564 1.795859099170403 1.7191213440296071 1.719121344140671 1.71912134425712 1.7191213443795217 1.696051956713387 1.6960519568498649 1.696051956994799 1.6960519571493533 1.6793907327970579 1.6793907329756121 1.6793907331695193 1.6793907333819287 1.6478305294839575 1.6478305297487643 1.6478305300534357 1.6478305304153813 1.6231591022557672 1.6231591028722896 1.6231591038531372 1.6231591057626238 1.4102603927036785 1.4102603950347914 1.286745701462116 1.2867457030514178 1.3587307677505704 1.2109111507749728 1.0254720000000002 1.0254720000000002 1.115288 1.115288 1.115288 1.115288 1.244344 1.244344 1.244344 1.244344 1.2665800000000003 1.2665800000000003 1.2665800000000003 1.2665800000000003 1.3001520000000002 1.3001520000000002 1.3001520000000002 1.3001520000000002 1.308654 1.308654 1.308654 1.308654 1.157798 1.157798 1.157798 1.157798 1.1102739999999998 1.1102739999999998 1.1102739999999998 1.1102739999999998 1.109402 1.109402 1.109402 1.109402 1.200526 1.3727893830154385 1.507959419920288 1.507959419920288 1.8059701140349236 1.8059701140349236 1.8915582236631034 1.8915582236892723 1.9573545911770804 1.9573545912041446 1.9573545912316663 1.9573545912669401 1.8470032033646882 1.847003203401333 1.8470032034387402 1.8470032034769441 1.6344170399883657 1.6344170400284683 1.6344170400696565 1.6344170401119915 1.5399534890443207 1.763818281863819 1.7638182819048551 1.7638182819468926 1.7990631800187922 1.7990631800629164 1.7990631801081867 1.7990631801546737 1.7747121597458562 1.7747121597950715 1.7747121598799356 1.7747121599677405];
xold=[0.7139072588912293	0.7139072586200328	0.7139072583378366	0.7139072580436695	0.481250114996675	0.4812501146748985	0.4812501143371005	0.481250113981534	0.4113072564969112	0.4113072560991915	0.411307255676166	0.41130725522422895	0.36079296904853175	0.36079296852409115	0.3607929679531262	0.36079296732577165	0.26510725238717525	0.26510725159908	0.2651072506882636	0.26510724959968973	0.19030724826346254	0.1903072463714543	0.1903072433275645	0.1903072373570503	0.9247294956004355	0.924729485444608	1.2126417844399506	1.2126417777978336	1.391304621335794	1.7095558672926798	2.0697600000000005	2.0697600000000005	2.25104	2.25104	2.25104	2.25104	2.5115199999999995	2.5115199999999995	2.5115199999999995	2.5115199999999995	2.5564000000000004	2.5564000000000004	2.5564000000000004	2.5564000000000004	2.6241600000000007	2.6241600000000007	2.6241600000000007	2.6241600000000007	2.64132	2.64132	2.64132	2.64132	2.3368399999999996	2.3368399999999996	2.3368399999999996	2.3368399999999996	2.2409199999999996	2.2409199999999996	2.2409199999999996	2.2409199999999996	2.23916	2.23916	2.23916	2.23916	2.4230799999999997	2.0720530486317026	1.7669391176673137	1.7669391176673137	2.506584831816604	2.506584831816604	2.2465638023470262	2.2465638022825507	2.427266659300257	2.427266659233213	2.427266659164807	2.4272666590683505	2.1241980876391433	2.1241980875385016	2.124198087435626	2.1241980873304005	1.5403523731018924	1.540352372991363	1.5403523728779946	1.5403523727616348	1.280918086992198	0.616764403595723	0.6167644034775093	0.6167644033562396	0.723621546033268	0.7236215459054279	0.7236215457740179	0.7236215456388131	0.6497929741095216	0.6497929739659075	0.6497929737229762	0.6497929734713137];
x=xold;

%Data of master iteration 2. This will generate the output for iteration 3
%xold=[1.3154935248882969	1.3154935247332153	1.3154935245717128	1.3154935244032173	1.1238935242270784	1.1238935240425458	1.1238935238487493	1.1238935236446714	1.0662935234291129	1.0662935232006416	1.066293522957533	1.0662935226976755	1.0246935224184461	1.0246935221165179	1.0246935217875675	1.0246935214257995	0.945893521023131	0.9458935205676786	0.9458935200407348	0.9458935194101163	0.8842935186141142	0.8842935175169332	0.8842935157647823	0.8842935123379808	1.3148087566862345	1.3148087496289873	1.448302401196002	1.4483023966946438	1.6027023876241282	1.7386231835078991	1.8816	1.8816	2.0463999999999998	2.0463999999999998	2.0463999999999998	2.0463999999999998	2.2832	2.2832	2.2832	2.2832	2.3240000000000003	2.3240000000000003	2.3240000000000003	2.3240000000000003	2.3856	2.3856	2.3856	2.3856	2.4012000000000002	2.4012000000000002	2.4012000000000002	2.4012000000000002	2.1244	2.1244	2.1244	2.1244	2.0372	2.0372	2.0372	2.0372	2.0356	2.0356	2.0356	2.0356	2.2028	2.0598232059434425	1.9263295665762477	1.9263295665762477	2.5655295665762483	2.5655295665762483	2.4296087700179134	2.429608769983618	2.5820087699486667	2.582008769913031	2.5820087698766794	2.5820087698224627	2.3264087697671223	2.3264087697106133	2.3264087696528817	2.326408769593869	1.834008769533516	1.834008769471761	1.8340087694085383	1.834008769343778	1.6152087692774049	1.2354935259378235	1.2354935258679742	1.235493525796246	1.3234935257225335	1.3234935256467202	1.3234935255686813	1.3234935254882765	1.2626935254053537	1.2626935253197458	1.2626935251811422	1.2626935250374578]
%u=[1.2891697517764595 1.2891697518732874 1.28916975197401 1.2891697520789696 1.237848323558432 1.2378483236731872 1.237848323793637 1.2378483239204008 1.2224197526088227 1.2224197527505647 1.2224197529013003 1.2224197530623044 1.2112768960803457 1.211276896267084 1.2112768964703289 1.211276896693564 1.1901697540621952 1.1901697543423797 1.190169754666052 1.1901697550526842 1.1736697555217972 1.173669756193506 1.1736697572774133 1.17366975940597 0.9458374413713408 0.9458374446849428 0.8352547080790251 0.8352547102747441 0.881023286238135 0.7558778622306349 0.61152 0.61152 0.6650799999999999 0.6650799999999999 0.6650799999999999 0.6650799999999999 0.74204 0.74204 0.74204 0.74204 0.7553000000000001 0.7553000000000001 0.7553000000000001 0.7553000000000001 0.77532 0.77532 0.77532 0.77532 0.78039 0.78039 0.78039 0.78039 0.6904300000000001 0.6904300000000001 0.6904300000000001 0.6904300000000001 0.6620899999999998 0.6620899999999998 0.6620899999999998 0.6620899999999998 0.66157 0.66157 0.66157 0.66157 0.7159099999999999 0.8556792771700092 0.974862832810405 0.974862832810405 1.16433997573576 1.16433997573576 1.2603702913309653 1.2603702913546293 1.3033688628370381 1.303368862861651 1.3033688628867661 1.3033688629214404 1.2312531486222094 1.2312531486584026 1.2312531486954077 1.231253148733267 1.0923260058324327 1.0923260058722586 1.0923260059131374 1.0923260059551272 1.0305931488232525 1.2677411796865943 1.267741179728239 1.2677411797709417 1.2913126084139994 1.291312608458966 1.2913126085051614 1.2913126085526625 1.2750268942965777 1.2750268943469827 1.2750268944337975 1.2750268945237075]
%xmean=[0.629666514220608 0.6296665142398952 0.6296665142598663 0.6296665142805782 0.6022950856719713 0.6022950856944602 0.6022950857180117 0.6022950857427365 0.5940665143233789 0.5940665143508855 0.5940665143800666 0.5940665144111422 0.5881236572895687 0.5881236573253429 0.5881236573641124 0.5881236574064639 0.5768665145737606 0.5768665146262191 0.5768665146864194 0.5768665147577424 0.5680665148288544 0.5680665149519727 0.5680665151598044 0.5680665155749604 0.523741819714458 0.5237418194994365 0.47990590867702626 0.47990590862206606 0.506374480050199 0.4491894539845845 0.37632 0.37632 0.40928 0.40928 0.40928 0.40928 0.45664 0.45664 0.45664 0.45664 0.46480000000000005 0.46480000000000005 0.46480000000000005 0.46480000000000005 0.47712000000000004 0.47712000000000004 0.47712000000000004 0.47712000000000004 0.48024000000000006 0.48024000000000006 0.48024000000000006 0.48024000000000006 0.42488000000000004 0.42488000000000004 0.42488000000000004 0.42488000000000004 0.40743999999999997 0.40743999999999997 0.40743999999999997 0.40743999999999997 0.40712000000000004 0.40712000000000004 0.40712000000000004 0.40712000000000004 0.44055999999999995 0.5088408801417307 0.5612776160985289 0.5612776160985289 0.6708547590238838 0.6708547590238838 0.6989246763399217 0.6989246763464381 0.7228732478113715 0.7228732478181668 0.7228732478251061 0.7228732478326718 0.6827075335057702 0.682707533513709 0.6827075335218483 0.6827075335302014 0.6053303905991909 0.6053303906081393 0.6053303906174066 0.6053303906270161 0.5709475334619547 0.618237942655506 0.618237942662226 0.6182379426690648 0.630809371275266 0.6308093712823262 0.6308093712895021 0.6308093712968008 0.6221236569992545 0.6221236570068556 0.6221236570243686 0.6221236570424364]
%x=xold

%Data of master iteration 1. This will generate the output for iteration 2
%xold=[1.31725	1.31725	1.31725	1.31725	1.1975	1.1975	1.1975	1.1975	1.1615	1.1615	1.1615	1.1615	1.1355	1.1355	1.1355	1.1355	1.08625	1.08625	1.08625	1.08625	1.04775	1.04775	1.04775	1.04775	1.0795	1.0795	1.0795	1.0795	1.176	1.176	1.176	1.176	1.279	1.279	1.279	1.279	1.427	1.427	1.427	1.427	1.4525000000000001	1.4525000000000001	1.4525000000000001	1.4525000000000001	1.491	1.491	1.491	1.491	1.5007499999999998	1.5007499999999998	1.5007499999999998	1.5007499999999998	1.32775	1.32775	1.32775	1.32775	1.2732499999999998	1.2732499999999998	1.2732499999999998	1.2732499999999998	1.2722499999999999	1.2722499999999999	1.2722499999999999	1.2722499999999999	1.37675	1.37675	1.37675	1.37675	1.77625	1.77625	1.77625	1.77625	1.8715	1.8715	1.8715	1.8715	1.71175	1.71175	1.71175	1.71175	1.404	1.404	1.404	1.404	1.26725	1.26725	1.26725	1.26725	1.32225	1.32225	1.32225	1.32225	1.28425	1.28425	1.28425	1.28425]
%u=[0.6595032375558516 0.6595032376333922 0.6595032377141438 0.6595032377983914 0.6355532378864608 0.635553237978727 0.6355532380756254 0.6355532381776642 0.6283532382854438 0.6283532383996792 0.6283532385212336 0.6283532386511622 0.6231532387907769 0.623153238941741 0.6231532391062163 0.6231532392871003 0.6133032394884346 0.6133032397161606 0.6133032399796325 0.6133032402949418 0.6056032406929428 0.6056032412415334 0.6056032421176089 0.6056032438310096 0.4220956216568828 0.42209562518550625 0.35534879940199887 0.35534880165267796 0.37464880618793595 0.3066884082460504 0.2352 0.2352 0.25579999999999997 0.25579999999999997 0.25579999999999997 0.25579999999999997 0.2854 0.2854 0.2854 0.2854 0.29050000000000004 0.29050000000000004 0.29050000000000004 0.29050000000000004 0.2982 0.2982 0.2982 0.2982 0.30015 0.30015 0.30015 0.30015 0.26555 0.26555 0.26555 0.26555 0.25464999999999993 0.25464999999999993 0.25464999999999993 0.25464999999999993 0.25444999999999995 0.25444999999999995 0.25444999999999995 0.25444999999999995 0.27535 0.34683839702827857 0.41358521671187615 0.41358521671187615 0.4934852167118762 0.4934852167118762 0.5614456149910436 0.5614456150081911 0.5804956150256666 0.5804956150434843 0.58049561506166 0.5804956150887685 0.5485456151164392 0.5485456151446936 0.5485456151735594 0.5485456152030656 0.48699561523324186 0.4869956152641194 0.4869956152957308 0.4869956153281111 0.45964561536129767 0.6495032370310883 0.6495032370660129 0.649503237101877 0.6605032371387334 0.6605032371766397 0.6605032372156594 0.6605032372558617 0.652903237297323 0.652903237340127 0.6529032374094289 0.6529032374812711]
%xmean=[0.6595032375558516 0.6595032376333922 0.6595032377141438 0.6595032377983914 0.6355532378864608 0.635553237978727 0.6355532380756254 0.6355532381776642 0.6283532382854438 0.6283532383996792 0.6283532385212336 0.6283532386511622 0.6231532387907769 0.623153238941741 0.6231532391062163 0.6231532392871003 0.6133032394884346 0.6133032397161606 0.6133032399796325 0.6133032402949418 0.6056032406929428 0.6056032412415334 0.6056032421176089 0.6056032438310096 0.4220956216568828 0.42209562518550625 0.35534879940199887 0.35534880165267796 0.37464880618793595 0.3066884082460504 0.2352 0.2352 0.25579999999999997 0.25579999999999997 0.25579999999999997 0.25579999999999997 0.2854 0.2854 0.2854 0.2854 0.29050000000000004 0.29050000000000004 0.29050000000000004 0.29050000000000004 0.2982 0.2982 0.2982 0.2982 0.30015 0.30015 0.30015 0.30015 0.26555 0.26555 0.26555 0.26555 0.25464999999999993 0.25464999999999993 0.25464999999999993 0.25464999999999993 0.25444999999999995 0.25444999999999995 0.25444999999999995 0.25444999999999995 0.27535 0.34683839702827857 0.41358521671187615 0.41358521671187615 0.4934852167118762 0.4934852167118762 0.5614456149910436 0.5614456150081911 0.5804956150256666 0.5804956150434843 0.58049561506166 0.5804956150887685 0.5485456151164392 0.5485456151446936 0.5485456151735594 0.5485456152030656 0.48699561523324186 0.4869956152641194 0.4869956152957308 0.4869956153281111 0.45964561536129767 0.6495032370310883 0.6495032370660129 0.649503237101877 0.6605032371387334 0.6605032371766397 0.6605032372156594 0.6605032372558617 0.652903237297323 0.652903237340127 0.6529032374094289 0.6529032374812711]
%x=xold

disp(size(xmean))
disp(size(u))
disp(size(xold))

data = -1*xold' + xmean' + u';

temp = -xold' + xmean' + u';

disp('XOLD + XMEAN')
disp(temp);
disp('END')
%disp('XOLD * -1')
%disp(-1*xold')
%disp('XOLD * -1')

%disp(data)


aa=[1.5886411870845472 1.5886411874476059 1.5886411878250661 1.5886411882181672 1.7191442495041072 1.7191442499332563 1.7191442503835026 1.7191442508571084 1.7583769043210398 1.7583769048499736 1.7583769054121317 1.7583769060121732 1.7867116004652384 1.7867116011600492 1.7867116019155835 1.7867116027445216 1.8403840525185444 1.8403840535560687 1.840384054752556 1.8403840561783888 1.8823412007262748 1.8823412031796187 1.8823412071012966 1.8823412147622272 0.9499538484355807 0.9499538599400321 0.5255949104052562 0.5255949180302578 0.4451336279272119 -0.043611427973369254 -0.6303360000000002 -0.6303360000000002 -0.6855440000000002 -0.6855440000000002 -0.6855440000000002 -0.6855440000000002 -0.7648719999999996 -0.7648719999999996 -0.7648719999999996 -0.7648719999999996 -0.77854 -0.77854 -0.77854 -0.77854 -0.7991760000000006 -0.7991760000000006 -0.7991760000000006 -0.7991760000000006 -0.8044019999999998 -0.8044019999999998 -0.8044019999999998 -0.8044019999999998 -0.7116739999999995 -0.7116739999999995 -0.7116739999999995 -0.7116739999999995 -0.6824619999999999 -0.6824619999999999 -0.6824619999999999 -0.6824619999999999 -0.681926 -0.681926 -0.681926 -0.681926 -0.7379379999999998 -0.1821535597708348 0.27411688936285716 0.27411688936285716 -0.05898457948251701 -0.05898457948251701 0.27618235364821553 0.2761823537413646 0.18407366021686578 0.1840736603134252 0.1840736604117592 0.18407366054408936 0.33855517046802364 0.3385551706057619 0.3385551707464467 0.3385551708902206 0.6361557010424064 0.636155701193315 0.636155701348181 0.6361557015072211 0.7683957422731911 1.6431309804453207 1.6431309806039618 1.643130980766604 1.5831922055903167 1.583192205761439 1.583192205937194 1.583192206117872 1.624604451085613 1.6246044512772528 1.6246044516030973 1.6246044519404599];
%aa = [-0.2072179117915709 -0.20721791152285718 -0.20721791124339017 -0.20721791095223585 2.2905474499956835E-5 2.2905792585237172E-5 2.2906126382504777E-5 2.290647758679265E-5 0.06232494760765289 0.062324948000108626 0.06232494841733266 0.06232494886281992 0.1073208676681805 0.10732086818443703 0.10732086874606422 0.10732086936259283 0.19255352303458695 0.1925535238073045 0.19255352469912035 0.1925535257630075 0.25918209847050755 0.25918210030732924 0.25918210324815943 0.2591821089996034 -0.4603065442680978 -0.46030653509475933 -0.7611507910568598 -0.76115078502116 -0.9135971398233586 -1.254522578748342 -1.6558080000000004 -1.6558080000000004 -1.8008320000000002 -1.8008320000000002 -1.8008320000000002 -1.8008320000000002 -2.0092159999999994 -2.0092159999999994 -2.0092159999999994 -2.0092159999999994 -2.0451200000000003 -2.0451200000000003 -2.0451200000000003 -2.0451200000000003 -2.0993280000000007 -2.0993280000000007 -2.0993280000000007 -2.0993280000000007 -2.113056 -2.113056 -2.113056 -2.113056 -1.8694719999999996 -1.8694719999999996 -1.8694719999999996 -1.8694719999999996 -1.7927359999999997 -1.7927359999999997 -1.7927359999999997 -1.7927359999999997 -1.791328 -1.791328 -1.791328 -1.791328 -1.9384639999999997 -1.5549429427862733 -1.2338425305574308 -1.2338425305574308 -1.8649546935174406 -1.8649546935174406 -1.615375870014888 -1.6153758699479077 -1.7732809309602147 -1.7732809308907194 -1.7732809308199071 -1.7732809307228508 -1.5084480328966645 -1.5084480327955712 -1.5084480326922936 -1.5084480325867236 -0.9982613389459594 -0.9982613388351533 -0.9982613387214755 -0.9982613386047704 -0.7715577467711296 -0.12068730141849837 -0.12068730130089322 -0.1206873011802887 -0.21587097442847536 -0.2158709743014775 -0.21587097417099266 -0.21587097403680178 -0.15010770866024326 -0.15010770851781863 -0.15010770827683817 -0.15010770802728068]';
%aa = double(aa);
%disp(aa)
disp('JAVA data Array start')
%disp(aa)
%temp=double(temp);

%result = aa'-temp;
%disp(result);
disp('JAVA data Array End')

yalmip=[-0.271391187084547
  -0.271391187447606
  -0.271391187825066
  -0.271391188218167
  -0.521644249504107
  -0.521644249933256
  -0.521644250383503
  -0.521644250857109
  -0.596876904321040
  -0.596876904849973
  -0.596876905412132
  -0.596876906012173
  -0.651211600465238
  -0.651211601160049
  -0.651211601915583
  -0.651211602744521
  -0.754134052518544
  -0.754134053556069
  -0.754134054752556
  -0.754134056178389
  -0.834591200726275
  -0.834591203179619
  -0.834591207101297
  -0.834591214762227
   0.129546151564419
   0.129546140059968
   0.553905089594744
   0.553905081969742
   0.730866372072788
   1.219611427973369
   1.806336000000000
   1.806336000000000
   1.964544000000000
   1.964544000000000
   1.964544000000000
   1.964544000000000
   2.191871999999999
   2.191871999999999
   2.191871999999999
   2.191871999999999
   2.231040000000000
   2.231040000000000
   2.231040000000000
   2.231040000000000
   2.290176000000001
   2.290176000000001
   2.290176000000001
   2.290176000000001
   2.305152000000000
   2.305152000000000
   2.305152000000000
   2.305152000000000
   2.039423999999999
   2.039423999999999
   2.039423999999999
   2.039423999999999
   1.955712000000000
   1.955712000000000
   1.955712000000000
   1.955712000000000
   1.954176000000000
   1.954176000000000
   1.954176000000000
   1.954176000000000
   2.114687999999999
   1.558903559770835
   1.102633110637143
   1.102633110637143
   1.835234579482517
   1.835234579482517
   1.500067646351784
   1.500067646258635
   1.687426339783134
   1.687426339686574
   1.687426339588241
   1.687426339455910
   1.373194829531976
   1.373194829394238
   1.373194829253553
   1.373194829109780
   0.767844298957594
   0.767844298806685
   0.767844298651819
   0.767844298492779
   0.498854257726809
  -0.375880980445321
  -0.375880980603962
  -0.375880980766604
  -0.260942205590317
  -0.260942205761439
  -0.260942205937194
  -0.260942206117872
  -0.340354451085613
  -0.340354451277253
  -0.340354451603097
  -0.340354451940460]

%disp(xold(:,end)+xmean+u)
%cvx_begin
 %       variable x_n(T)
        
        %minimize( (-1 * price' * x_n) + (rho/2 * sum(x_n.*x_n))  )
        %TODO UNCOMMENT
        %minimize( (-1 * price' * x_n) + (rho/2 * sum((x_n - xold' + xmean' + u').*(x_n - xold' + xmean' + u')))  )
        %subject to
         %   -xa_min >= x_n >= -xa_max  %TODO: Excluded this constraint because I get a wrong answer. Don't know why yet.
        
            
%cvx_end

%disp(x_n)
%disp(double(x_n)-yalmip)
%disp(sum(yalmip))
%disp(sum(x_n))
%disp(sum(x_n))
%TODO: UNCOMMENT END



%YALMIP SOLVER
xtemp= sdpvar(T,1,'full');
   
Cost= (-1 * price' * xtemp) + (rho/2 * sum((xtemp - xold' + xmean' + u').*(xtemp - xold' + xmean' + u')));
                
                %B*x<=[Smax,Smin], A*x== R', d'*-4 <= x <= d'*4
Constraints=[-xa_min >= xtemp >= -xa_max];
                
                %ops = sdpsettings('solver','gurobi','verbose',0);
                ops = sdpsettings('verbose',0);
                %model = export(Constraints,Cost, ops)
                %saveampl(Constraints, Cost, 'mymodel.mod')
                savecplexlp(Constraints, Cost, 'mymodelCplex.mod')
  %              sol=solvesdp(Constraints,Cost,ops);
   %             if sol.problem == 0
    %             x = double(xtemp);
     %            disp('SOLUTION')
      %           disp(x)
       %         end
        %        disp('END')

end
